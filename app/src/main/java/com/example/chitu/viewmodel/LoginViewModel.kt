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
import retrofit2.HttpException
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
                if (response.code == 200 && response.data != null) {
                    tokenManager.saveToken(response.data.token, response.data.userId)
                    _loginState.value = LoginUiState.Success(response.data.token, response.data.userId)
                } else {
                    _loginState.value = LoginUiState.Error(response.message)
                }
            } catch (e: HttpException) {
                val errorMessage = parseHttpException(e)
                _loginState.value = LoginUiState.Error(errorMessage)
            } catch (e: IOException) {
                _loginState.value = LoginUiState.Error("网络异常，请检查网络连接")
            } catch (e: Exception) {
                _loginState.value = LoginUiState.Error(e.message ?: "登录失败")
            }
        }
    }

    /**
     * 解析 HTTP 错误响应体中的后端 message 字段
     */
    private fun parseHttpException(e: HttpException): String {
        return try {
            val errorBody = e.response()?.errorBody()?.string()
            if (errorBody != null) {
                val json = JSONObject(errorBody)
                json.optString("message", "登录失败，请重试")
            } else {
                "登录失败，请重试"
            }
        } catch (ex: Exception) {
            "登录失败，请重试"
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