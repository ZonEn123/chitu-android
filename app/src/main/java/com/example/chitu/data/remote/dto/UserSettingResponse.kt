package com.example.chitu.data.remote.dto

data class UserSettingResponse(
    val darkMode: Int,          // 0-浅色 1-深色
    val soundEnabled: Int,      // 0-关闭 1-开启
    val vibrationEnabled: Int,  // 0-关闭 1-开启
    val reminderInterval: Int   // 疲劳提醒间隔（分钟）
)