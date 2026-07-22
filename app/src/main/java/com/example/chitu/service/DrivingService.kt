package com.example.chitu.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.chitu.MainActivity
import com.example.chitu.data.local.DataStoreManager
import com.example.chitu.viewmodel.DrivingViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    companion object {
        private const val TAG = "DrivingService"
        private const val CHANNEL_ID = "driving_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_RESTORE = "ACTION_RESTORE"
    }

    // ==================== 生命周期 ====================

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: 服务创建")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action} flags=$flags startId=$startId")
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "收到 ACTION_START，开始驾驶")
                startDriving()
            }
            ACTION_STOP -> {
                Log.d(TAG, "收到 ACTION_STOP，结束驾驶")
                stopDriving()
            }
            ACTION_RESTORE -> {
                Log.d(TAG, "收到 ACTION_RESTORE，恢复驾驶状态")
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
                    delay(1000L)
                }
            }
        } else {
            Log.d(TAG, "没有驾驶记录，无需恢复（timestamp=$timestamp）")
        }
    }

    // ==================== 核心业务 ====================

    private fun startDriving() {
        Log.d(TAG, "startDriving: 开始驾驶")
        if (_isDriving.value) {
            Log.d(TAG, "已经在驾驶中，忽略")
            return
        }

        serviceStartTime = System.currentTimeMillis()
        _startTimestamp.value = serviceStartTime
        _isDriving.value = true
        _elapsedSeconds.value = 0

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
                delay(1000L)
            }
        }
    }

    private fun stopDriving() {
        if (!_isDriving.value) return

        timerJob?.cancel()
        timerJob = null

        _isDriving.value = false
        _elapsedSeconds.value = 0
        _startTimestamp.value = 0L

        // ✅ 清除 DataStore
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