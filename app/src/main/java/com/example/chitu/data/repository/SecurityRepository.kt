package com.example.chitu.data.repository

import com.example.chitu.data.remote.RetrofitClient
import com.example.chitu.data.remote.dto.ApiResponse

class SecurityRepository {

    suspend fun getSecurityQuestion(phone: String): ApiResponse<Any> {
        val response = RetrofitClient.authApi.getSecurityQuestion(phone)
        return response.body() ?: ApiResponse(500, "请求失败", null)
    }

    suspend fun verifySecurity(phone: String, answer: String): ApiResponse<Any> {
        val response = RetrofitClient.authApi.verifySecurity(
            mapOf("phone" to phone, "answer" to answer)
        )
        return response.body() ?: ApiResponse(500, "请求失败", null)
    }

    suspend fun resetPassword(phone: String, newPassword: String): ApiResponse<Any> {
        val response = RetrofitClient.authApi.resetPassword(
            mapOf("phone" to phone, "newPassword" to newPassword)
        )
        return response.body() ?: ApiResponse(500, "请求失败", null)
    }

    suspend fun changePassword(token: String, answer: String, newPassword: String): ApiResponse<Any> {
        val response = RetrofitClient.authApi.changePassword(
            "Bearer $token",
            mapOf("answer" to answer, "newPassword" to newPassword)
        )
        return response.body() ?: ApiResponse(500, "请求失败", null)
    }

    suspend fun getMySecurityQuestion(token: String): ApiResponse<Any> {
        val response = RetrofitClient.authApi.getMySecurityQuestion("Bearer $token")
        return response.body() ?: ApiResponse(500, "请求失败", null)
    }
}
