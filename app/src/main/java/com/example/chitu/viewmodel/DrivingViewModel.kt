package com.example.chitu.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitu.data.local.DataStoreManager
import com.example.chitu.service.DrivingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 驾驶监测 ViewModel
 *
 * 职责：
 * 1. 订阅 DrivingService 的状态
 * 2. 提供 startDriving / stopDriving 方法
 * 3. 持有驾驶状态供 UI 使用
 */
class DrivingViewModel(
    private val context: Context
) : ViewModel() {

    // 单例 Service 状态管理器
    private val serviceState = DrivingServiceState.getInstance()

    // 暴露给 UI 的状态
    val isDriving: StateFlow<Boolean> = serviceState.isDriving
    val elapsedSeconds: StateFlow<Int> = serviceState.elapsedSeconds
    val startTimestamp: StateFlow<Long> = serviceState.startTimestamp


    /**
     * 开始驾驶
     */
    fun startDriving(reminderInterval: Int = 240) {
        val intent = Intent(context, DrivingService::class.java).apply {
            action = DrivingService.ACTION_START
            putExtra("reminder_interval", reminderInterval)
        }
        context.startForegroundService(intent)
    }

    /**
     * 结束驾驶
     */
    fun stopDriving() {
        val intent = Intent(context, DrivingService::class.java).apply {
            action = DrivingService.ACTION_STOP
        }
        context.startForegroundService(intent)
    }

    /**
     * 检查并恢复驾驶状态
     *
     * 在 APP 启动时调用：
     * 1. 读取 DataStore 中保存的开始时间戳
     * 2. 如果存在有效记录，主动启动 Service（ACTION_RESTORE）
     * 3. Service 恢复通知栏和计时
     *
     * ViewModel 只负责「决定要不要恢复」，不直接修改 StateFlow
     */
    fun checkAndRestoreDriving() {
        viewModelScope.launch(Dispatchers.IO) {
            val dataStore = DataStoreManager(context)
            val timestamp = dataStore.getStartTimestamp()
            Log.d("DrivingViewModel", "checkAndRestoreDriving: timestamp=$timestamp")
            if (timestamp != null && timestamp > 0) {
                Log.d("DrivingViewModel", "发现驾驶记录，启动 Service(ACTION_RESTORE)")
                val intent = Intent(context, DrivingService::class.java).apply {
                    action = DrivingService.ACTION_RESTORE
                }
                context.startForegroundService(intent)
            } else {
                Log.d("DrivingViewModel", "没有驾驶记录，无需恢复")
            }
        }
    }

    /**
     * 格式化时间显示
     */
    fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    /**
     * 单例：管理 Service 状态
     * 这样 Service 和 ViewModel 通过同一个对象通信
     */
    object DrivingServiceState {
        private val _isDriving = MutableStateFlow(false)
        val isDriving: StateFlow<Boolean> = _isDriving.asStateFlow()

        private val _elapsedSeconds = MutableStateFlow(0)
        val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

        private val _startTimestamp = MutableStateFlow(0L)
        val startTimestamp: StateFlow<Long> = _startTimestamp.asStateFlow()

        fun getInstance(): DrivingServiceState = this

        // Service 调用，更新状态
        fun update(isDriving: Boolean, seconds: Int, timestamp: Long) {
            _isDriving.value = isDriving
            _elapsedSeconds.value = seconds
            _startTimestamp.value = timestamp
        }
    }
}