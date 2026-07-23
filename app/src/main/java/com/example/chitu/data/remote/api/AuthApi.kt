package com.example.chitu.data.remote.api

import com.example.chitu.data.remote.dto.ApiResponse
import com.example.chitu.data.remote.dto.LoginData
import com.example.chitu.data.remote.dto.LoginRequest
import com.example.chitu.data.remote.dto.LoginResponse
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

interface AuthApi {
    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): ApiResponse<LoginData>   // ✅ 统一为 ApiResponse

    @POST("/api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<RegisterData>>   // ✅ 改为 Response<T>

    /**
     * 获取个人信息
     * 需要 Token 放在 Header 中
     *
     * 为什么用 @Header 而不是写在请求体里？
     * 因为 Token 属于认证凭证，标准做法是放在 HTTP Header 的 Authorization 字段中。
     */
    @GET("/api/user/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): ApiResponse<UserProfileResponse>

    @PUT("/api/user/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): ApiResponse<Any>

    @GET("/api/user/setting")
    suspend fun getSetting(
        @Header("Authorization") token: String
    ): ApiResponse<UserSettingResponse>

    // 在 AuthApi 中添加
    @PUT("/api/user/setting")
    suspend fun updateSetting(
        @Header("Authorization") token: String,
        @Body request: UpdateSettingRequest
    ): ApiResponse<Any>
    // ✅ 同步行程
    @POST("/api/trips/sync")
    suspend fun syncTrip(
        @Header("Authorization") token: String,
        @Body request: TripSyncRequest
    ): ApiResponse<Any>
}