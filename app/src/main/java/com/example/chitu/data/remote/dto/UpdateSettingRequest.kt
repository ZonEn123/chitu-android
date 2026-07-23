package com.example.chitu.data.remote.dto

data class UpdateSettingRequest(
    val darkMode: Int? = null,
    val soundEnabled: Int? = null,
    val vibrationEnabled: Int? = null,
    val reminderInterval: Int? = null
)