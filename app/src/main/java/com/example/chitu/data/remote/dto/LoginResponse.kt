package com.example.chitu.data.remote.dto

data class LoginResponse(
    val code: Int,
    val message: String,
    val data: LoginData?
)

//data class LoginData(
//    val token: String,
//    val userId: Long,
//    val phone: String,
//    val role: Int
//)