package com.example.chitu.data.remote.dto

data class UserSettingResponse(
    val darkMode: Int,
    val soundEnabled: Int,
    val vibrationEnabled: Int,
    val reminderInterval: Int
)