package com.example.chitu.data.remote.dto

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)