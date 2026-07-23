package com.example.chitu.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.example.chitu.MainActivity
import com.example.chitu.data.local.DataStoreManager
import com.example.chitu.data.local.TokenManager
import com.example.chitu.data.sync.TripSyncManager
import com.example.chitu.viewmodel.DrivingViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.chitu.data.local.database.TripLogDatabase
import com.example.chitu.data.local.entity.TripLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/** 定位状态快照 */
data class LocationState(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val accuracy: Float,
    val timestamp: Long
)

class DrivingService : Service() {

    // ==================== 对外暴露的状态 ====================

    private val _isDriving = MutableStateFlow(false)
    val isDriving: StateFlow<Boolean> = _isDriving.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _startTimestamp = MutableStateFlow(0L)
    val startTimestamp: StateFlow<Long> = _startTimestamp.asStateFlow()

    // ==================== 内部属性 ====================

    private var timerJob: Job? = null
    private var serviceStartTime: Long = 0L
    private val dataStore by lazy { DataStoreManager(this) }

    // Service 专用协程作用域，避免协程泄漏
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ✅ 疲劳提醒相关
    private var _hasAlerted = MutableStateFlow(false)
    private var fatigueLimitSeconds: Int = 240 * 60  // 会被用户设置覆盖

    // ==================== 行程定位 ====================
    private lateinit var locationClient: AMapLocationClient

    /** 当前位置的可靠快照（每次有效回调更新） */
    private var currentLocation: LocationState? = null

    /** 起点快照（稳定确认后锁定） */
    private var startFix: LocationState? = null

    /** 终点快照（停止驾驶时缓存） */
    private var endFix: LocationState? = null

    /** 稳定定位计数器（累计连续 accuracy <= 50 的次数） */
    private var stableFixCount = 0

    /** 兼容旧字段引用（由 LocationState 提供） */
    private val startLatitude: Double get() = startFix?.latitude ?: 0.0
    private val startLongitude: Double get() = startFix?.longitude ?: 0.0
    private val endLatitude: Double get() = endFix?.latitude ?: 0.0
    private val endLongitude: Double get() = endFix?.longitude ?: 0.0
    private val startLocation: String get() = startFix?.address ?: lastValidAddress
    private val endLocation: String get() = endFix?.address ?: lastValidAddress

    // 有效地址缓存（仅保存非空地址，防止空值覆盖）
    private var lastValidAddress = "未知位置"

    private var totalDistance = 0f

    private var isFirstLocation = true

    companion object {
        private const val TAG = "DrivingService"
        private const val CHANNEL_ID = "driving_channel"
        private const val NOTIFICATION_ID = 1001
        private const val FATIGUE_NOTIFICATION_ID = 2001

        // 连续稳定定位确认次数
        private const val REQUIRED_STABLE_FIXES = 3
        // 停止驾驶时等待主动定位的超时时间
        private const val LOCATION_TIMEOUT_MS = 3000L

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_RESTORE = "ACTION_RESTORE"
    }

    // ==================== 生命周期 ====================

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: 服务创建")
        createNotificationChannel()
        createFatigueNotificationChannel()
        // 定位不在这里初始化，在 startDriving 中按需启动
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action} flags=$flags startId=$startId")
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "收到 ACTION_START，开始驾驶")
                // ✅ 从 Intent 读取用户设置的疲劳阈值
                fatigueLimitSeconds = intent.getIntExtra("reminder_interval", 240) * 60
                Log.d(TAG, "疲劳阈值: ${fatigueLimitSeconds / 60} 分钟")
                startDriving()
            }
            ACTION_STOP -> {
                Log.d(TAG, "收到 ACTION_STOP，结束驾驶")
                stopDriving()
            }
            ACTION_RESTORE -> {
                Log.d(TAG, "收到 ACTION_RESTORE，恢复驾驶状态")
                // ✅ 恢复时也读取阈值
                fatigueLimitSeconds = intent.getIntExtra("reminder_interval", 240) * 60
                serviceScope.launch {
                    restoreDrivingState()
                }
            }
            else -> {
                Log.d(TAG, "收到未知 action（${intent?.action}），尝试恢复驾驶状态")
                serviceScope.launch {
                    restoreDrivingState()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: 服务销毁")
        timerJob?.cancel()
        timerJob = null
        serviceScope.cancel()
        // 如果定位已初始化，停止定位
        if (::locationClient.isInitialized) {
            locationClient.stopLocation()
            locationClient.onDestroy()
        }
    }

    // ==================== 恢复驾驶状态 ====================

    private suspend fun restoreDrivingState() {
        val timestamp = dataStore.getStartTimestamp()
        Log.d(TAG, "restoreDrivingState: 读取到时间戳=$timestamp")
        if (timestamp != null && timestamp > 0) {
            // 恢复驾驶状态
            serviceStartTime = timestamp
            _startTimestamp.value = timestamp
            _isDriving.value = true
            _hasAlerted.value = false  // ✅ 重置提醒标志

            // 计算已过去的时间
            val elapsed = (System.currentTimeMillis() - timestamp) / 1000
            _elapsedSeconds.value = elapsed.toInt()
            Log.d(TAG, "恢复驾驶状态: 已驾驶 ${elapsed}秒")

            // 同步更新单例状态
            DrivingViewModel.DrivingServiceState.getInstance().update(
                isDriving = true,
                seconds = elapsed.toInt(),
                timestamp = timestamp
            )

            // 重新启动前台服务
            startForeground(NOTIFICATION_ID, createNotification(elapsed.toInt()))
            Log.d(TAG, "通知栏已重建")

            // 重新启动计时协程
            timerJob = serviceScope.launch {
                while (_isDriving.value) {
                    val now = System.currentTimeMillis()
                    val elapsed = (now - serviceStartTime) / 1000
                    _elapsedSeconds.value = elapsed.toInt()

                    DrivingViewModel.DrivingServiceState.getInstance().update(
                        isDriving = true,
                        seconds = elapsed.toInt(),
                        timestamp = serviceStartTime
                    )

                    updateNotification(elapsed.toInt())

                    // ✅ 疲劳驾驶提醒检查（恢复后也要检查）
                    if (elapsed >= fatigueLimitSeconds && !_hasAlerted.value) {
                        _hasAlerted.value = true
                        Log.d(TAG, "⚠️ 恢复后触发疲劳驾驶提醒！已驾驶 ${elapsed / 60} 分钟")
                        sendFatigueAlert()
                    }

                    delay(1000L)
                }
            }
        } else {
            Log.d(TAG, "没有驾驶记录，无需恢复（timestamp=$timestamp）")
        }
    }

    // ==================== 定位 ====================

    private fun initLocation() {

        locationClient = AMapLocationClient(applicationContext)

        val option = AMapLocationClientOption().apply {

            locationMode =
                AMapLocationClientOption.AMapLocationMode.Hight_Accuracy

            interval = 2000

            isNeedAddress = true

            isOnceLocation = false
        }


        locationClient.setLocationOption(option)

        locationClient.setLocationListener(locationListener)

        locationClient.startLocation()


        Log.d(TAG, "定位启动")
    }



    private val locationListener =
        AMapLocationListener { location ->

            if (location == null) {
                Log.w(TAG, "定位返回 null")
                return@AMapLocationListener
            }

            val errorCode = location.errorCode
            if (errorCode != 0) {
                val errorMsg = when (errorCode) {
                    12 -> "GPS 未开启"
                    32 -> "Key 无效"
                    61 -> "网络定位失败"
                    62 -> "服务器返回错误"
                    63 -> "网络异常"
                    161 -> "定位成功但无地址"
                    else -> "未知错误"
                }
                Log.w(TAG, "定位失败: errorCode=$errorCode, $errorMsg, info=${location.errorInfo}")
                return@AMapLocationListener
            }

            val lat = location.latitude
            val lng = location.longitude
            val address = location.address
            val accuracy = location.accuracy

            Log.d(
                "LocationDebug",
                "回调: 精度=${accuracy}米, 地址=$address, " +
                        "lat=$lat, lng=$lng, errorCode=$errorCode"
            )

            // 1. 缓存非空地址（永不丢失）
            if (!address.isNullOrBlank()) {
                lastValidAddress = address
            }

            // 2. 创建本次定位快照
            val locationState = LocationState(
                latitude = lat,
                longitude = lng,
                address = lastValidAddress,
                accuracy = accuracy,
                timestamp = System.currentTimeMillis()
            )
            currentLocation = locationState

            // 3. 稳定起点确认（连续 accuracy <= 50 才锁定）
            if (startFix == null) {
                if (accuracy <= 50f) {
                    stableFixCount++
                    Log.d(TAG, "稳定定位: $stableFixCount/$REQUIRED_STABLE_FIXES")

                    if (stableFixCount >= REQUIRED_STABLE_FIXES) {
                        startFix = locationState
                        Log.d(
                            "LocationDebug",
                            "✅ 起点已稳定确认: ${startFix?.address}, " +
                                    "纬度=${startFix?.latitude}, 精度=${startFix?.accuracy}米"
                        )
                    }
                } else {
                    stableFixCount = 0
                    Log.d(TAG, "精度不足，重置稳定计数: ${accuracy}米")
                }
                return@AMapLocationListener
            }

            // 4. 后续定位：累计里程 + 更新终点
            val fromLat = endFix?.latitude ?: startFix!!.latitude
            val fromLng = endFix?.longitude ?: startFix!!.longitude
            val distance = calculateDistance(fromLat, fromLng, lat, lng)

            // 仅在精度 ≤ 50 时计算移动距离（过滤低精度漂移）
            if (accuracy <= 50f && distance in 5f..100f) {
                totalDistance += distance
                Log.d(TAG, "有效移动:${distance}米 累计:${totalDistance}米")
            } else {
                Log.d(TAG, "过滤抖动/跳点:${distance}米 精度:${accuracy}米")
            }

            // 更新终点快照
            endFix = locationState
        }



    private fun calculateDistance(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Float {


        val result = FloatArray(1)


        android.location.Location.distanceBetween(
            lat1,
            lng1,
            lat2,
            lng2,
            result
        )


        return result[0]

    }

    // ==================== 核心业务 ====================

    private fun startDriving() {
        Log.d(TAG, "startDriving: 开始驾驶")
        if (_isDriving.value) {
            Log.d(TAG, "已经在驾驶中，忽略")
            return
        }

        // 初始化行程数据
        totalDistance = 0f
        isFirstLocation = true
        stableFixCount = 0
        currentLocation = null
        startFix = null
        endFix = null

        Log.d(TAG, "📍 [开始驾驶] 行程定位已重置")

        serviceStartTime = System.currentTimeMillis()
        _startTimestamp.value = serviceStartTime
        _isDriving.value = true
        _elapsedSeconds.value = 0
        _hasAlerted.value = false  // ✅ 重置疲劳提醒标志

        // ✅ 启动定位（如果尚未初始化）
        if (!::locationClient.isInitialized) {
            initLocation()
        }

        // ✅ 保存到 DataStore
        serviceScope.launch {
            Log.d(TAG, "保存时间戳到 DataStore: $serviceStartTime")
            dataStore.saveStartTimestamp(serviceStartTime)
        }

        // 同步更新单例状态
        DrivingViewModel.DrivingServiceState.getInstance().update(
            isDriving = true,
            seconds = 0,
            timestamp = serviceStartTime
        )

        startForeground(NOTIFICATION_ID, createNotification(0))

        timerJob = serviceScope.launch {
            while (_isDriving.value) {
                val now = System.currentTimeMillis()
                val elapsed = (now - serviceStartTime) / 1000
                _elapsedSeconds.value = elapsed.toInt()

                DrivingViewModel.DrivingServiceState.getInstance().update(
                    isDriving = true,
                    seconds = elapsed.toInt(),
                    timestamp = serviceStartTime
                )

                updateNotification(elapsed.toInt())

                // ✅ 疲劳驾驶提醒检查
                if (elapsed >= fatigueLimitSeconds && !_hasAlerted.value) {
                    _hasAlerted.value = true
                    Log.d(TAG, "⚠️ 触发疲劳驾驶提醒！已驾驶 ${elapsed / 60} 分钟")
                    sendFatigueAlert()
                }

                delay(1000L)
            }
        }
    }

    private fun stopDriving() {
        if (!_isDriving.value) return

        if (::locationClient.isInitialized) {
            locationClient.stopLocation()
        }

        // ✅ 组装行程数据
        val endTime = System.currentTimeMillis()
        val durationSeconds = ((endTime - serviceStartTime) / 1000).toInt()

        // 生成客户端唯一ID（幂等同步用）
        val clientId = UUID.randomUUID().toString()

        // 地址兜底：防止空字符串入库
        val finalStart = startLocation.ifBlank { "未知位置" }
        val finalEnd = endLocation.ifBlank { "未知位置" }

        val tripLog = TripLog(
            clientId = clientId,
            startTime = serviceStartTime,
            endTime = endTime,
            durationSeconds = durationSeconds,
            startLocation = finalStart,
            endLocation = finalEnd,
            distanceMeters = totalDistance,
            tripStatus = 1,
            fatigueFlag = if (_hasAlerted.value) 1 else 0,
            remark = "",
            startLatitude = startLatitude,
            startLongitude = startLongitude,
            endLatitude = endLatitude,
            endLongitude = endLongitude,
            syncStatus = 0
        )

        Log.d(TAG, "📦 行程记录: $tripLog")

        Log.d(
            "LocationDebug",
            "保存行程: startLocation=$startLocation, endLocation=$endLocation, " +
                    "distance=$totalDistance, duration=$durationSeconds"
        )

        // ✅ 保存到 Room + 立即同步（使用独立协程，不绑定 Service 生命周期）
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TripLogDatabase.getInstance(this@DrivingService)
                val newId = db.tripLogDao().insert(tripLog)
                tripLog.id = newId  // 手动写回 Room 生成的 ID
                Log.d(TAG, "✅ 行程已保存到本地数据库, id=$newId")

                // 立即尝试同步到云端
                val tokenManager = TokenManager(this@DrivingService)
                val token = tokenManager.getToken()
                if (!token.isNullOrBlank()) {
                    val syncManager = TripSyncManager(this@DrivingService)
                    val success = syncManager.syncTripNow(token, tripLog)
                    if (success) {
                        Log.d(TAG, "✅ 行程已同步到云端")
                    } else {
                        Log.d(TAG, "⚠️ 行程同步失败，等待重试")
                    }
                } else {
                    Log.d(TAG, "⚠️ 无 Token，暂不同步")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 保存或同步行程失败", e)
            }
        }

        timerJob?.cancel()
        timerJob = null

        _isDriving.value = false
        _elapsedSeconds.value = 0
        _startTimestamp.value = 0L
        _hasAlerted.value = false

        serviceScope.launch {
            dataStore.clearStartTimestamp()
        }

        DrivingViewModel.DrivingServiceState.getInstance().update(
            isDriving = false,
            seconds = 0,
            timestamp = 0L
        )

        stopForeground(true)
        stopSelf()
    }

    // ==================== 疲劳提醒 ====================

    private fun createFatigueNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "fatigue_alert",
                "疲劳驾驶提醒",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "疲劳驾驶超时提醒"
                setSound(null, null)
                enableVibration(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private suspend fun sendFatigueAlert() {
        // 从 DataStore 读取用户设置（带异常保护，防止协程崩溃导致提醒永久失效）
        var soundEnabled = 1
        var vibrationEnabled = 1
        try {
            val dataStore = DataStoreManager(this)
            soundEnabled = dataStore.getSoundEnabledOnce()
            vibrationEnabled = dataStore.getVibrationEnabledOnce()
            Log.d(TAG, "提醒设置 sound=$soundEnabled vibration=$vibrationEnabled")
        } catch (e: Exception) {
            Log.e(TAG, "读取设置失败，使用默认值", e)
        }

        // 1. 震动（判断开关）
        if (vibrationEnabled == 1) {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(1000)
            }
        }

        // 2. 声音（独立控制，使用 Ringtone 播放，与通知完全分离）
        if (soundEnabled == 1) {
            try {
                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(this, defaultSoundUri)
                ringtone?.let { r ->
                    r.play()
                    serviceScope.launch {
                        delay(2500L)
                        r.stop()
                        Log.d(TAG, "系统音效播放完成，资源已释放")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "播放系统音效失败", e)
            }
        }

        // 3. 通知（永远发送，完全静音）
        //    Android 13+：如果用户拒绝了通知权限，不弹通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "无通知权限，跳过疲劳提醒通知")
                return
            }
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "fatigue_alert")
            .setContentTitle("⚠️ 疲劳驾驶提醒")
            .setContentText("您已连续驾驶超过设定时间，请立即停车休息！")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSilent(true)
            .build()

        NotificationManagerCompat.from(this).notify(FATIGUE_NOTIFICATION_ID, notification)
    }

    // ==================== 通知栏 ====================

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "驾驶监测",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "赤兔驾驶监测服务"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(seconds: Int): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeStr = formatTime(seconds)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚛 赤兔驾驶中")
            .setContentText("已驾驶: $timeStr")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(seconds: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        val notification = createNotification(seconds)
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }
}