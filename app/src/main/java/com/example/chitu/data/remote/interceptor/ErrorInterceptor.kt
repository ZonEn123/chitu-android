package com.example.chitu.data.remote.interceptor

import com.example.chitu.data.remote.exception.ApiException
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject

class ErrorInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        // 如果响应成功（2xx），直接返回
        if (response.isSuccessful) {
            return response
        }

        // 非 2xx 响应，尝试提取错误信息
        val errorBody = response.body?.string() ?: ""
        val message = try {
            // 尝试解析 JSON 中的 message 字段
            val json = JSONObject(errorBody)
            json.optString("message", "请求失败，请稍后重试")
        } catch (e: Exception) {
            // 如果不是 JSON 格式，使用状态码描述
            "请求失败 (${response.code})"
        }

        // 抛出包含错误信息的异常
        throw ApiException(response.code, message)
    }
}