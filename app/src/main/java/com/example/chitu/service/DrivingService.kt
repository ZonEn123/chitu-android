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
import com.example.chitu.viewmodel.DrivingViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.chitu.data.local.database.TripLogDatabase
import com.example.chitu.data.local.entity.TripLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    private var startLatitude = 0.0
    private var startLongitude = 0.0

    private var endLatitude = 0.0
    private var endLongitude = 0.0

    private var startLocation = "未知位置"
    private var endLocation = "未知位置"

    private var totalDistance = 0f

    private var lastLatitude = 0.0
    private var lastLongitude = 0.0

    private var isFirstLocation = true

    companion object {
        private const val TAG = "DrivingService"
        private const val CHANNEL_ID = "driving_channel"
        private const val NOTIFICATION_ID = 1001
        private const val FATIGUE_NOTIFICATION_ID = 2001

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

            // 过滤无效定位 + GPS 精度差（室内测试放宽到50m，室外稳定后通常在10m以内）
            if (location != null && location.errorCode == 0 && location.accuracy < 50) {

                val lat = location.latitude
                val lng = location.longitude

                if (isFirstLocation) {

                    startLatitude = lat
                    startLongitude = lng

                    startLocation =
                        location.address ?: "未知位置"

                    lastLatitude = lat
                    lastLongitude = lng

                    isFirstLocation = false

                    Log.d(
                        TAG,
                        "记录起点:$startLocation 精度:${location.accuracy}米"
                    )


                } else {

                    if (lastLatitude != 0.0) {

                        val distance =
                            calculateDistance(
                                lastLatitude,
                                lastLongitude,
                                lat,
                                lng
                            )

                        // ✅ GPS 漂移过滤：丢弃 <5m 的抖动和 >100m 的异常跳点
                        if (distance in 5f..100f) {
                            totalDistance += distance

                            Log.d(
                                TAG,
                                "有效移动:${distance}米 累计:${totalDistance}米"
                            )
                        } else {
                            Log.d(
                                TAG,
                                "过滤GPS抖动:${distance}米 精度:${location.accuracy}米"
                            )
                        }
                    }

                    lastLatitude = lat
                    lastLongitude = lng

                    // 保存最新位置作为终点
                    endLatitude = lat
                    endLongitude = lng

                    endLocation =
                        location.address ?: "未知位置"
                }
            } else {
                Log.d(
                    TAG,
                    "定位精度不足:${location?.accuracy}米 errorCode=${location?.errorCode}"
                )
            }
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
        startLocation = "未知位置"
        endLocation = "未知位置"

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

        val tripLog = TripLog(
            startTime = serviceStartTime,
            endTime = endTime,
            durationSeconds = durationSeconds,
            startLocation = startLocation,
            endLocation = endLocation,
            distanceMeters = totalDistance,
            tripStatus = 1,  // 已完成
            fatigueFlag = if (_hasAlerted.value) 1 else 0,
            remark = ""
        )

        Log.d(TAG, "行程记录: $tripLog")

        // ✅ 保存到 Room 数据库
        serviceScope.launch {
            try {
                val db = TripLogDatabase.getInstance(this@DrivingService)
                db.tripLogDao().insert(tripLog)
                Log.d(TAG, "✅ 行程已保存到数据库")
            } catch (e: Exception) {
                Log.e(TAG, "❌ 保存行程失败", e)
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
                "fatigue_channel",
                "疲劳驾驶提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "疲劳驾驶超时提醒"
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun sendFatigueAlert() {
        // 1. 震动（1000ms）
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(1000)
        }

        // 2. 系统音效（默认通知铃声，播放 2 秒后自动释放）
        try {
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(this, defaultSoundUri)
            ringtone?.let { r ->
                r.play()
                // 系统通知音一般 1~2 秒，延时后释放资源
                serviceScope.launch {
                    delay(2500L)
                    r.stop()
                    Log.d(TAG, "系统音效播放完成，资源已释放")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放系统音效失败", e)
        }

        // 3. 系统通知（需要通知权限）
        //    Android 13+：如果用户拒绝了通知权限，只震动+音效，不弹通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "无通知权限，跳过疲劳提醒通知（震动和音效已触发）")
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

        val notification = NotificationCompat.Builder(this, "fatigue_channel")
            .setContentTitle("⚠️ 疲劳驾驶提醒")
            .setContentText("您已连续驾驶超过设定时间，请立即停车休息！")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
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