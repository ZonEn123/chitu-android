package com.example.chitu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_log")
data class TripLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,           // 开始时间戳
    val endTime: Long,             // 结束时间戳
    val durationSeconds: Int,      // 驾驶时长（秒）
    val startLocation: String,     // 起点名称
    val endLocation: String,       // 终点名称
    val distanceMeters: Float,     // 里程（米）
    val tripStatus: Int = 1,       // 0-进行中 1-已完成 2-异常结束
    val fatigueFlag: Int = 0,      // 0-否 1-是（是否触发疲劳）
    val remark: String = ""        // 备注
)