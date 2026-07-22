package com.example.chitu.data.remote.dto

data class UpdateProfileRequest(
    val nickname: String? = null,
    val phone: String? = null,
    val age: Int? = null,
    val gender: Int? = null,
    val emergencyPhone: String? = null,
    val securityQuestion: String? = null,
    val securityAnswer: String? = null
)