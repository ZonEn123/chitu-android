package com.example.chitu.data.remote.dto

data class LoginData(
    val token: String,
    val userId: Long,
    val phone: String,
    val role: Int
)