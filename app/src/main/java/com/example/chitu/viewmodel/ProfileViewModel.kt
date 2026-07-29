package com.example.chitu.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitu.data.local.TokenManager
import com.example.chitu.data.remote.RetrofitClient
import com.example.chitu.data.remote.dto.UpdateProfileRequest
import com.example.chitu.data.remote.dto.UserProfileResponse
import com.example.chitu.data.remote.dto.UserSettingResponse
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

// ============================================================
// 页面UI状态
// ============================================================

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val data: UserProfileResponse) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

// ============================================================
// 编辑临时数据
// ============================================================

data class EditedProfileData(
    val nickname: String = "",
    val age: String = "",
    val gender: String = "",
    val emergencyPhone: String = "",
    val securityQuestion: String = "",
    val securityAnswer: String = ""
)

// ============================================================
// Dialog状态管理
// ============================================================

sealed class ProfileDialogState {
    object None : ProfileDialogState()
    object SaveConfirm : ProfileDialogState()
    object DiscardConfirm : ProfileDialogState()
    object SecurityReminder : ProfileDialogState()
}

// ============================================================
// UserProfile扩展
// ============================================================

fun UserProfileResponse.isSecuritySet(): Boolean {
    return !securityQuestion.isNullOrBlank() && !securityAnswer.isNullOrBlank()
}

// ============================================================
// ProfileViewModel V2.0
// ============================================================

class ProfileViewModel(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _settingState = MutableStateFlow(
        UserSettingResponse(darkMode = 0, soundEnabled = 1, vibrationEnabled = 1, reminderInterval = 240)
    )
    val settingState: StateFlow<UserSettingResponse> = _settingState.asStateFlow()

    fun loadSetting() {
        Log.d("ProfileViewModel", "loadSetting: 开始获取用户设置")
        viewModelScope.launch {
            val token = tokenManager.getToken()
            if (token.isNullOrBlank()) {
                Log.w("ProfileViewModel", "loadSetting: token为空，跳过")
                return@launch
            }
            try {
                val httpRsp = RetrofitClient.authApi.getSetting("Bearer $token")
                val body = httpRsp.body()
                Log.d("ProfileViewModel", "loadSetting: 响应 code=${body?.code} data=${body?.data}")
                if (body?.code == 200 && body.data != null) {
                    Log.d("ProfileViewModel", "loadSetting: 成功, reminderInterval=${body.data.reminderInterval}")
                    _settingState.value = body.data
                } else {
                    Log.w("ProfileViewModel", "loadSetting: 业务失败, code=${body?.code} message=${body?.message}")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "loadSetting: 异常", e)
            }
        }
    }

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _originalData = MutableStateFlow<UserProfileResponse?>(null)
    val originalData = _originalData.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing = _isEditing.asStateFlow()

    private val _editedData = MutableStateFlow(EditedProfileData())
    val editedData = _editedData.asStateFlow()

    private val _hasChanges = MutableStateFlow(false)
    val hasChanges = _hasChanges.asStateFlow()

    private val _dialogState = MutableStateFlow<ProfileDialogState>(ProfileDialogState.None)
    val dialogState = _dialogState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val token = tokenManager.getToken()
            if (token.isNullOrBlank()) {
                _uiState.value = ProfileUiState.Error("登录状态失效，请重新登录")
                return@launch
            }
            try {
                val httpRsp = RetrofitClient.authApi.getProfile("Bearer $token")
                val body = httpRsp.body()
                if (body?.code == 200 && body.data != null) {
                    val data = body.data
                    _originalData.value = data
                    _editedData.value = EditedProfileData(
                        nickname = data.nickname ?: "",
                        age = data.age?.toString() ?: "",
                        gender = when (data.gender) { 1 -> "男"; 0 -> "女"; else -> "" },
                        emergencyPhone = data.emergencyPhone ?: "",
                        securityQuestion = data.securityQuestion ?: "",
                        securityAnswer = data.securityAnswer ?: ""
                    )
                    _uiState.value = ProfileUiState.Success(data)
                    _hasChanges.value = false
                } else {
                    _uiState.value = ProfileUiState.Error(body?.message ?: "获取个人信息失败")
                }
            } catch (e: HttpException) {
                val errorMsg = try {
                    val rbody = e.response()?.errorBody()?.string()
                    if (rbody != null) JSONObject(rbody).optString("message", "获取信息失败")
                    else "获取信息失败"
                } catch (_: Exception) { "获取信息失败" }
                _uiState.value = ProfileUiState.Error(errorMsg)
            } catch (e: IOException) {
                _uiState.value = ProfileUiState.Error("网络异常，请检查网络连接")
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "获取信息失败")
            }
        }
    }

    fun enterEditMode() {
        val data = _originalData.value
        if (data != null) {
            _editedData.value = EditedProfileData(
                nickname = data.nickname ?: "",
                age = data.age?.toString() ?: "",
                gender = when (data.gender) { 1 -> "男"; 0 -> "女"; else -> "" },
                emergencyPhone = data.emergencyPhone ?: "",
                securityQuestion = data.securityQuestion ?: "",
                securityAnswer = data.securityAnswer ?: ""
            )
            _hasChanges.value = false
        }
        _isEditing.value = true
    }

    fun exitEditMode() {
        restoreEditData()
        _isEditing.value = false
        _hasChanges.value = false
        _dialogState.value = ProfileDialogState.None
    }

    private fun restoreEditData() {
        val data = _originalData.value ?: return
        _editedData.value = EditedProfileData(
            nickname = data.nickname ?: "",
            age = data.age?.toString() ?: "",
            gender = when (data.gender) { 1 -> "男"; 0 -> "女"; else -> "" },
            emergencyPhone = data.emergencyPhone ?: "",
            securityQuestion = data.securityQuestion ?: "",
            securityAnswer = data.securityAnswer ?: ""
        )
    }

    fun updateEditedData(block: EditedProfileData.() -> EditedProfileData) {
        val newData = _editedData.value.block()
        _editedData.value = newData
        checkHasChanges()
    }

    private fun checkHasChanges() {
        val original = _originalData.value ?: return
        val edited = _editedData.value
        _hasChanges.value = edited.nickname != (original.nickname ?: "") ||
            edited.age != (original.age?.toString() ?: "") ||
            edited.gender != when (original.gender) { 1 -> "男"; 0 -> "女"; else -> "" } ||
            edited.emergencyPhone != (original.emergencyPhone ?: "") ||
            ((original.securityQuestion.isNullOrBlank() && original.securityAnswer.isNullOrBlank()) &&
             edited.securityQuestion.isNotBlank() && edited.securityAnswer.isNotBlank())
    }

    private fun validateProfile(): String? {
        val data = _editedData.value
        if (data.nickname.isBlank()) return "昵称不能为空"
        if (data.age.isNotBlank()) {
            val age = data.age.toIntOrNull()
            if (age == null || age < 0 || age > 120) return "年龄格式错误"
        }
        if (data.gender.isNotBlank() && data.gender != "男" && data.gender != "女") return "性别只能填写男或女"
        if (data.emergencyPhone.isNotBlank() && !data.emergencyPhone.matches(Regex("^\\d+$"))) return "紧急联系人号码格式错误"
        val original = _originalData.value
        val securityNotSet = original?.securityQuestion.isNullOrBlank() && original?.securityAnswer.isNullOrBlank()
        if (securityNotSet && (data.securityQuestion.isNotBlank() || data.securityAnswer.isNotBlank())) {
            if (data.securityQuestion.isBlank() || data.securityAnswer.isBlank()) return "密保问题和答案必须同时填写"
        }
        return null
    }

    fun saveProfile() { _dialogState.value = ProfileDialogState.SaveConfirm }

    fun confirmSave(context: Context, onSuccess: () -> Unit) {
        val error = validateProfile()
        if (error != null) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val token = tokenManager.getToken()
            if (token.isNullOrBlank()) {
                Toast.makeText(context, "登录失效", Toast.LENGTH_SHORT).show()
                _isSaving.value = false; return@launch
            }
            try {
                val edited = _editedData.value
                val original = _originalData.value
                val request = UpdateProfileRequest(
                    nickname = if (edited.nickname != (original?.nickname ?: "")) edited.nickname else null,
                    age = edited.age.toIntOrNull(),
                    gender = when (edited.gender) { "男" -> 1; "女" -> 0; else -> null },
                    emergencyPhone = if (edited.emergencyPhone != (original?.emergencyPhone ?: "")) edited.emergencyPhone else null,
                    securityQuestion = if (edited.securityQuestion.isNotBlank()) edited.securityQuestion else null,
                    securityAnswer = if (edited.securityAnswer.isNotBlank()) edited.securityAnswer else null
                )
                val httpRsp = RetrofitClient.authApi.updateProfile("Bearer $token", request)
                val body = httpRsp.body()
                if (body?.code == 200) {
                    Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                    loadProfile(); _isEditing.value = false; onSuccess()
                } else {
                    Toast.makeText(context, body?.message ?: "保存失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: HttpException) {
                val errorMsg = try {
                    val rbody = e.response()?.errorBody()?.string()
                    if (rbody != null) JSONObject(rbody).optString("message", "请求失败") else "请求失败"
                } catch (_: Exception) { "请求失败" }
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(context, "网络异常，请检查网络连接", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "请求失败", Toast.LENGTH_SHORT).show()
            }
            _isSaving.value = false
            _dialogState.value = ProfileDialogState.None
        }
    }

    fun cancelSave() { _dialogState.value = ProfileDialogState.None }
    fun showDiscardDialog() { _dialogState.value = ProfileDialogState.DiscardConfirm }

    fun confirmDiscard() {
        restoreEditData(); _hasChanges.value = false
        _dialogState.value = ProfileDialogState.None; _isEditing.value = false
    }

    fun cancelDiscard() { _dialogState.value = ProfileDialogState.None }
    fun showSecurityReminder() { _dialogState.value = ProfileDialogState.SecurityReminder }

    fun confirmSecurityReminder() {
        _dialogState.value = ProfileDialogState.None; enterEditMode()
        _editedData.value = _editedData.value.copy(securityQuestion = "", securityAnswer = "")
        _hasChanges.value = false
    }

    fun cancelSecurityReminder() { _dialogState.value = ProfileDialogState.None }
    fun resetDialog() { _dialogState.value = ProfileDialogState.None }

    suspend fun logout(context: android.content.Context) {
        tokenManager.clear()
        com.example.chitu.data.local.DataStoreManager(context).clearSettings()
    }

    override fun onCleared() { super.onCleared() }
}
