package com.example.chitu.data.remote.dto

data class RegisterRequest(
    val phone: String,
    val password: String,
    val nickname: String? = null
)