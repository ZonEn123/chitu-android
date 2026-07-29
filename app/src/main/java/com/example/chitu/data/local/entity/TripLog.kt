package com.example.chitu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_log")
data class TripLog(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    // 用户ID（用于用户数据隔离）
    val userId: Long = 0,

    // 客户端唯一ID（幂等同步用）
    val clientId: String = "",

    // 服务器端ID（同步成功后填充）
    val serverId: Long? = null,

    val startTime: Long,           // 开始时间戳
    val endTime: Long,             // 结束时间戳
    val durationSeconds: Int,      // 驾驶时长（秒）
    val startLocation: String,     // 起点名称
    val endLocation: String,       // 终点名称
    val distanceMeters: Float,     // 里程（米）
    val tripStatus: Int = 1,       // 0-进行中 1-已完成 2-异常结束
    val fatigueFlag: Int = 0,      // 0-否 1-是（是否触发疲劳）
    val remark: String = "",       // 备注

    // 起点的纬度、经度
    val startLatitude: Double = 0.0,
    val startLongitude: Double = 0.0,

    // 终点的纬度、经度
    val endLatitude: Double = 0.0,
    val endLongitude: Double = 0.0,

    // 同步状态（0-未同步 1-已同步 2-同步失败）
    var syncStatus: Int = 0
)
