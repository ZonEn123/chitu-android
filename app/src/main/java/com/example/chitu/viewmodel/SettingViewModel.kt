package com.example.chitu.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitu.data.local.DataStoreManager
import com.example.chitu.data.local.TokenManager
import com.example.chitu.data.remote.RetrofitClient
import com.example.chitu.data.remote.dto.UpdateSettingRequest
import com.example.chitu.data.remote.dto.UserSettingResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class SettingViewModel(
    private val context: Context,
    private val tokenManager: TokenManager
) : ViewModel() {

    // ==================== 设置数据状态 ====================

    private val _settings = MutableStateFlow<UserSettingResponse?>(null)
    val settings: StateFlow<UserSettingResponse?> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    // ==================== 加载设置 ====================

    fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true

            val token = tokenManager.getToken()
            if (token.isNullOrBlank()) {
                _isLoading.value = false
                return@launch
            }

            try {
                val response = RetrofitClient.authApi.getSetting("Bearer $token")
                if (response.code == 200 && response.data != null) {
                    _settings.value = response.data
                }
            } catch (e: Exception) {
                // 静默失败，使用默认值
            }

            _isLoading.value = false
        }
    }

    // ==================== 保存设置 ====================

    fun saveSetting(
        darkMode: Int? = null,
        soundEnabled: Int? = null,
        vibrationEnabled: Int? = null,
        reminderInterval: Int? = null
    ) {
        viewModelScope.launch {
            _saveState.value = SaveState.Loading

            val token = tokenManager.getToken()
            if (token.isNullOrBlank()) {
                _saveState.value = SaveState.Error("登录失效，请重新登录")
                return@launch
            }

            try {
                val request = UpdateSettingRequest(
                    darkMode = darkMode,
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled,
                    reminderInterval = reminderInterval
                )

                val response = RetrofitClient.authApi.updateSetting("Bearer $token", request)

                if (response.code == 200) {
                    // 保存成功，重新加载设置
                    loadSettings()
                    // 同步写入 DataStore 本地缓存
                    DataStoreManager(context).saveSettings(
                        darkMode = darkMode,
                        soundEnabled = soundEnabled,
                        vibrationEnabled = vibrationEnabled,
                        reminderInterval = reminderInterval
                    )
                    _saveState.value = SaveState.Success
                } else {
                    _saveState.value = SaveState.Error(response.message ?: "保存失败")
                }
            } catch (e: HttpException) {
                val errorMsg = try {
                    val body = e.response()?.errorBody()?.string()
                    if (body != null) JSONObject(body).optString("message", "保存失败")
                    else "保存失败"
                } catch (_: Exception) { "保存失败" }
                _saveState.value = SaveState.Error(errorMsg)
            } catch (e: IOException) {
                _saveState.value = SaveState.Error("网络异常，请检查网络连接")
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "保存失败")
            }
        }
    }

    // ==================== 重置保存状态 ====================

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }

    // ============================================================
    // 保存状态密封类
    // ============================================================

    sealed class SaveState {
        object Idle : SaveState()
        object Loading : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }
}