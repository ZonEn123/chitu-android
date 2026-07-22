package com.example.chitu.data.remote.dto

/**
 * 用户个人信息响应数据类
 * 必须与后端的 UserProfileResponse 字段完全一致
 */
data class UserProfileResponse(
    val userId: Long,
    val phone: String,
    val nickname: String?,
    val role: Int,
    val status: Int,
    val avatar: String?,
    val age: Int?,
    val gender: Int,
    val emergencyPhone: String?,
    val securityQuestion: String?,
    val securityAnswer: String?
)