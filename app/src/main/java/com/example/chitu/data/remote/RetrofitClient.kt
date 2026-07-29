package com.example.chitu.data.remote

import com.example.chitu.data.remote.api.AuthApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    //iqqo13
    private const val BASE_URL = "http://10.210.221.165:8080/"

    //redmi80pro
    //private const val BASE_URL = "http://10.255.201.165:8080/"


    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        // ❌ 移除 ErrorInterceptor — 异常在 ViewModel 的 catch 块中统一处理
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }
}