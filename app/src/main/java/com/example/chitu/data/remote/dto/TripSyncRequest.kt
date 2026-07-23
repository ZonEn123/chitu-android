package com.example.chitu.data.remote.dto

data class TripSyncRequest(
    val clientId: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Int,
    val startLocation: String,
    val endLocation: String,
    val distanceMeters: Float,
    val tripStatus: Int,
    val fatigueFlag: Int,
    val remark: String,
    val startLatitude: Double = 0.0,
    val startLongitude: Double = 0.0,
    val endLatitude: Double = 0.0,
    val endLongitude: Double = 0.0
)
