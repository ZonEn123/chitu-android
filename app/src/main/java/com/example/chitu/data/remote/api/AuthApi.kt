package com.example.chitu.data.remote.api

import com.example.chitu.data.remote.dto.ApiResponse
import com.example.chitu.data.remote.dto.LoginData
import com.example.chitu.data.remote.dto.LoginRequest
import com.example.chitu.data.remote.dto.RegisterData
import com.example.chitu.data.remote.dto.RegisterRequest
import com.example.chitu.data.remote.dto.TripSyncRequest
import com.example.chitu.data.remote.dto.UpdateProfileRequest
import com.example.chitu.data.remote.dto.UpdateSettingRequest
import com.example.chitu.data.remote.dto.UserProfileResponse
import com.example.chitu.data.remote.dto.UserSettingResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface AuthApi {
    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginData>>

    @POST("/api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<RegisterData>>

    @GET("/api/user/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<ApiResponse<UserProfileResponse>>

    @PUT("/api/user/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse<Any>>

    @GET("/api/user/setting")
    suspend fun getSetting(
        @Header("Authorization") token: String
    ): Response<ApiResponse<UserSettingResponse>>

    @PUT("/api/user/setting")
    suspend fun updateSetting(
        @Header("Authorization") token: String,
        @Body request: UpdateSettingRequest
    ): Response<ApiResponse<Any>>

    @POST("/api/trips/sync")
    suspend fun syncTrip(
        @Header("Authorization") token: String,
        @Body request: TripSyncRequest
    ): Response<ApiResponse<Any>>

    @POST("/api/reminders")
    suspend fun saveReminder(
        @Header("Authorization") token: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<ApiResponse<Any>>

    // ========== 安全模块 ==========

    @GET("/api/auth/security-question")
    suspend fun getSecurityQuestion(
        @Query("phone") phone: String
    ): Response<ApiResponse<Any>>

    @POST("/api/auth/verify-security")
    suspend fun verifySecurity(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<ApiResponse<Any>>

    @PUT("/api/auth/reset-password")
    suspend fun resetPassword(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<ApiResponse<Any>>

    @PUT("/api/user/password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<ApiResponse<Any>>

    @GET("/api/user/security-question")
    suspend fun getMySecurityQuestion(
        @Header("Authorization") token: String
    ): Response<ApiResponse<Any>>
}
