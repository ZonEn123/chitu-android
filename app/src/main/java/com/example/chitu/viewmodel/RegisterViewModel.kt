package com.example.chitu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitu.data.remote.RetrofitClient
import com.example.chitu.data.remote.dto.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

// ============================================================
// 注册状态密封类
// ============================================================

sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    data class Success(val message: String) : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

// ============================================================
// RegisterViewModel
// ============================================================

class RegisterViewModel : ViewModel() {

    private val _registerState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    /**
     * 用户注册
     * @param phone 手机号
     * @param password 密码
     * @param nickname 昵称（可选）
     */
    fun register(phone: String, password: String, nickname: String? = null) {
        viewModelScope.launch {
            _registerState.value = RegisterUiState.Loading

            try {
                val response = RetrofitClient.authApi.register(
                    RegisterRequest(phone, password, nickname)
                )

                // response 是 Response<ApiResponse<RegisterData>>
                // HTTP 2xx — Gson 已解析 body
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 200) {
                        _registerState.value = RegisterUiState.Success(
                            apiResponse.message ?: "注册成功"
                        )
                    } else {
                        _registerState.value = RegisterUiState.Error(
                            apiResponse?.message ?: "注册失败，请重试"
                        )
                    }
                } else {
                    // 非 2xx 响应（如后端返回 HTTP 400 且 body 为 JSON）
                    // 此时 response.body() 为 null，需解析 errorBody()
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string()
                        if (errorBody != null) {
                            JSONObject(errorBody).optString("message", "注册失败，请重试")
                        } else {
                            "注册失败，请重试"
                        }
                    } catch (_: Exception) {
                        "注册失败，请重试"
                    }
                    _registerState.value = RegisterUiState.Error(errorMessage)
                }

            } catch (e: IOException) {
                // 网络异常
                _registerState.value = RegisterUiState.Error("网络异常，请检查网络连接")

            } catch (e: Exception) {
                // 其他未知异常
                _registerState.value = RegisterUiState.Error(e.message ?: "注册失败，请重试")
            }
        }
    }

    /**
     * 重置状态
     */
    fun resetState() {
        _registerState.value = RegisterUiState.Idle
    }
}