package com.example.chitu.data.remote.exception

/**
 * API 异常类
 * 用于在拦截器中包装后端返回的错误信息
 */
class ApiException(
    val code: Int,           // HTTP 状态码
    message: String          // 后端返回的 message 字段
) : Exception(message)