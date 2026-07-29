package com.example.chitu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitu.data.local.TokenManager
import com.example.chitu.data.repository.SecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 忘记密码步骤 */
sealed class ForgotStep {
    data object InputPhone : ForgotStep()
    data class VerifySecurity(val phone: String, val question: String) : ForgotStep()
    data class ResetPassword(val phone: String) : ForgotStep()
}

/** 安全模块 UI 状态 */
sealed class SecurityUiState {
    data object Idle : SecurityUiState()
    data object Loading : SecurityUiState()
    data class Question(val question: String) : SecurityUiState()
    data class Success(val message: String) : SecurityUiState()
    data class Error(val message: String) : SecurityUiState()
}

class SecurityViewModel(
    private val tokenManager: TokenManager? = null
) : ViewModel() {

    private val repository = SecurityRepository()
    private val _uiState = MutableStateFlow<SecurityUiState>(SecurityUiState.Idle)
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    // 忘记密码步骤驱动（单页面用）
    private val _forgotStep = MutableStateFlow<ForgotStep>(ForgotStep.InputPhone)
    val forgotStep: StateFlow<ForgotStep> = _forgotStep.asStateFlow()

    /** 查询密保问题（忘记密码用） */
    fun getSecurityQuestion(phone: String) {
        if (phone.length != 11) {
            _uiState.value = SecurityUiState.Error("请输入正确的手机号")
            return
        }
        viewModelScope.launch {
            _uiState.value = SecurityUiState.Loading
            try {
                val res = repository.getSecurityQuestion(phone)
                if (res.code == 200) {
                    val q = (res.data as? Map<*, *>)?.get("question") as? String ?: ""
                    _forgotStep.value = ForgotStep.VerifySecurity(phone, q)
                    _uiState.value = SecurityUiState.Question(q)
                } else {
                    _uiState.value = SecurityUiState.Error(res.message)
                }
            } catch (e: Exception) {
                _uiState.value = SecurityUiState.Error("网络异常，请稍后重试")
            }
        }
    }

    /** 验证密保答案（忘记密码用） */
    fun verifySecurity(phone: String, answer: String) {
        if (answer.isBlank()) {
            _uiState.value = SecurityUiState.Error("请输入密保答案")
            return
        }
        viewModelScope.launch {
            _uiState.value = SecurityUiState.Loading
            try {
                val res = repository.verifySecurity(phone, answer)
                if (res.code == 200) {
                    _forgotStep.value = ForgotStep.ResetPassword(phone)
                    _uiState.value = SecurityUiState.Idle
                } else {
                    _uiState.value = SecurityUiState.Error(res.message)
                }
            } catch (e: Exception) {
                _uiState.value = SecurityUiState.Error("网络异常，请稍后重试")
            }
        }
    }

    /** 重置密码（忘记密码） */
    fun resetPassword(phone: String, newPassword: String, confirmPassword: String) {
        if (newPassword.length < 6) {
            _uiState.value = SecurityUiState.Error("密码长度不能小于6位")
            return
        }
        if (newPassword != confirmPassword) {
            _uiState.value = SecurityUiState.Error("两次密码输入不一致")
            return
        }
        viewModelScope.launch {
            _uiState.value = SecurityUiState.Loading
            try {
                val res = repository.resetPassword(phone, newPassword)
                if (res.code == 200) {
                    _uiState.value = SecurityUiState.Success("密码修改成功，请重新登录")
                } else {
                    _uiState.value = SecurityUiState.Error(res.message)
                }
            } catch (e: Exception) {
                _uiState.value = SecurityUiState.Error("网络异常，请稍后重试")
            }
        }
    }

    /** 查询当前用户的密保问题（安全设置用） */
    fun getMySecurityQuestion() {
        viewModelScope.launch {
            _uiState.value = SecurityUiState.Loading
            try {
                val token = tokenManager?.getToken()
                if (token.isNullOrBlank()) {
                    _uiState.value = SecurityUiState.Error("登录已过期")
                    return@launch
                }
                // 直接调用 changePassword API 传入空密码仅用于验证 token
                // 重新从后端获取security question
                val res = repository.getMySecurityQuestion(token)
                if (res.code == 200) {
                    val q = (res.data as? Map<*, *>)?.get("question") as? String ?: ""
                    _uiState.value = SecurityUiState.Question(q)
                } else {
                    _uiState.value = SecurityUiState.Error(res.message)
                }
            } catch (e: Exception) {
                _uiState.value = SecurityUiState.Error("网络异常")
            }
        }
    }

    /** 登录后修改密码 */
    fun changePassword(answer: String, newPassword: String, confirmPassword: String) {
        if (newPassword.length < 6) {
            _uiState.value = SecurityUiState.Error("密码长度不能小于6位")
            return
        }
        if (newPassword != confirmPassword) {
            _uiState.value = SecurityUiState.Error("两次密码输入不一致")
            return
        }
        viewModelScope.launch {
            _uiState.value = SecurityUiState.Loading
            try {
                val token = tokenManager?.getToken()
                if (token.isNullOrBlank()) {
                    _uiState.value = SecurityUiState.Error("登录已过期，请重新登录")
                    return@launch
                }
                val res = repository.changePassword(token, answer, newPassword)
                if (res.code == 200) {
                    _uiState.value = SecurityUiState.Success("密码修改成功，请重新登录")
                } else {
                    _uiState.value = SecurityUiState.Error(res.message)
                }
            } catch (e: Exception) {
                _uiState.value = SecurityUiState.Error("网络异常，请稍后重试")
            }
        }
    }

    fun reset() {
        _uiState.value = SecurityUiState.Idle
        _forgotStep.value = ForgotStep.InputPhone
    }
}
