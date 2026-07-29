package com.example.chitu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitu.data.local.TokenManager
import com.example.chitu.data.remote.RetrofitClient
import com.example.chitu.data.remote.dto.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

class LoginViewModel(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState

    fun login(phone: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            try {
                val response = RetrofitClient.authApi.login(LoginRequest(phone, password))

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        when (body.code) {
                            200 -> {
                                // 登录成功
                                if (body.data != null) {
                                    tokenManager.saveToken(body.data.token, body.data.userId)
                                    _loginState.value = LoginUiState.Success(body.data.token, body.data.userId)
                                    return@launch
                                }
                            }
                            403 -> {
                                _loginState.value = LoginUiState.Error("账号已被封禁，请联系管理员")
                                return@launch
                            }
                            else -> {
                                // 其他业务错误（401手机号未注册/密码错误等）
                                _loginState.value = LoginUiState.Error(body.message)
                                return@launch
                            }
                        }
                    }
                }

                // HTTP 层错误（非 2xx），尝试解析 errorBody
                val errorMsg = parseErrorResponse(response.code(), response.errorBody()?.string())
                _loginState.value = LoginUiState.Error(errorMsg)

            } catch (e: IOException) {
                _loginState.value = LoginUiState.Error("网络异常，请检查网络连接")
            } catch (e: Exception) {
                _loginState.value = LoginUiState.Error(e.message ?: "登录失败")
            }
        }
    }

    private fun parseErrorResponse(httpCode: Int, errorBody: String?): String {
        if (errorBody != null) {
            try {
                val json = JSONObject(errorBody)
                val msg = json.optString("message", null)
                if (msg != null) return msg
            } catch (_: Exception) {}
        }
        return when (httpCode) {
            401 -> "手机号或密码错误"
            403 -> "账号已被封禁，请联系管理员"
            500 -> "服务器繁忙，请稍后重试"
            else -> "登录失败，请重试"
        }
    }

    fun resetState() {
        _loginState.value = LoginUiState.Idle
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val token: String, val userId: Long) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
