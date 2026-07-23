package com.example.chitu.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitu.data.local.database.TripLogDatabase
import com.example.chitu.data.local.entity.TripLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 驾驶统计数据
 */
data class DrivingStatistics(
    val totalDurationSeconds: Int = 0,      // 总驾驶时长（秒）
    val totalDistanceMeters: Float = 0f,    // 总里程（米）
    val totalTrips: Int = 0,                // 总行程次数
    val totalFatigue: Int = 0,              // 疲劳次数
    val weeklyData: List<DailyDrivingData> = emptyList()  // 近7天数据
)

/**
 * 每日驾驶数据（用于趋势图）
 */
data class DailyDrivingData(
    val date: String,           // "MM-dd" 格式
    val durationSeconds: Int,   // 当天驾驶秒数
    val timestamp: Long         // 用于排序
)

class StatisticsViewModel(
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatisticsUiState>(StatisticsUiState.Loading)
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val db by lazy { TripLogDatabase.getInstance(context) }

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = StatisticsUiState.Loading

            try {
                val allTrips = db.tripLogDao().getAllTripsFromDb()

                if (allTrips.isEmpty()) {
                    _uiState.value = StatisticsUiState.Empty
                    return@launch
                }

                // 1. 计算四项核心指标
                var totalDuration = 0
                var totalDistance = 0f
                var totalFatigue = 0

                allTrips.forEach { trip ->
                    totalDuration += trip.durationSeconds
                    totalDistance += trip.distanceMeters
                    if (trip.fatigueFlag == 1) totalFatigue++
                }

                // 2. 计算近7天数据
                val calendar = Calendar.getInstance()
                val today = Calendar.getInstance()
                today.set(Calendar.HOUR_OF_DAY, 0)
                today.set(Calendar.MINUTE, 0)
                today.set(Calendar.SECOND, 0)
                today.set(Calendar.MILLISECOND, 0)

                val weeklyMap = mutableMapOf<String, DailyDrivingData>()

                // 初始化近7天（从今天往前推6天）
                for (i in 6 downTo 0) {
                    val day = Calendar.getInstance()
                    day.add(Calendar.DAY_OF_YEAR, -i)
                    day.set(Calendar.HOUR_OF_DAY, 0)
                    day.set(Calendar.MINUTE, 0)
                    day.set(Calendar.SECOND, 0)
                    day.set(Calendar.MILLISECOND, 0)

                    val dateStr = SimpleDateFormat("MM-dd", Locale.getDefault()).format(day.time)
                    weeklyMap[dateStr] = DailyDrivingData(
                        date = dateStr,
                        durationSeconds = 0,
                        timestamp = day.timeInMillis
                    )
                }

                // 遍历所有行程，累加到对应日期
                allTrips.forEach { trip ->
                    val tripDate = Date(trip.startTime)
                    val tripCalendar = Calendar.getInstance().apply { time = tripDate }
                    tripCalendar.set(Calendar.HOUR_OF_DAY, 0)
                    tripCalendar.set(Calendar.MINUTE, 0)
                    tripCalendar.set(Calendar.SECOND, 0)
                    tripCalendar.set(Calendar.MILLISECOND, 0)

                    val dateStr = SimpleDateFormat("MM-dd", Locale.getDefault()).format(tripDate)

                    // 只统计近7天内的数据
                    if (weeklyMap.containsKey(dateStr)) {
                        val existing = weeklyMap[dateStr]!!
                        weeklyMap[dateStr] = existing.copy(
                            durationSeconds = existing.durationSeconds + trip.durationSeconds
                        )
                    }
                }

                // 转换为列表并按日期排序
                val weeklyData = weeklyMap.values.sortedBy { it.timestamp }

                val statistics = DrivingStatistics(
                    totalDurationSeconds = totalDuration,
                    totalDistanceMeters = totalDistance,
                    totalTrips = allTrips.size,
                    totalFatigue = totalFatigue,
                    weeklyData = weeklyData
                )

                _uiState.value = StatisticsUiState.Success(statistics)

            } catch (e: Exception) {
                _uiState.value = StatisticsUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    // 刷新数据
    fun refresh() {
        loadStatistics()
    }
}

sealed class StatisticsUiState {
    object Loading : StatisticsUiState()
    data class Success(val data: DrivingStatistics) : StatisticsUiState()
    object Empty : StatisticsUiState()
    data class Error(val message: String) : StatisticsUiState()
}

// 扩展函数：在 Dao 中添加
// 由于 Room 的 Flow 不能直接返回 List，需要添加一个 suspend 方法