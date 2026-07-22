package com.example.chitu.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitu.data.local.TokenManager
import com.example.chitu.data.remote.RetrofitClient
import com.example.chitu.data.remote.dto.UpdateProfileRequest
import com.example.chitu.data.remote.dto.UserProfileResponse
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
    // ❌ phone 已删除
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
    return !securityQuestion.isNullOrBlank() &&
            !securityAnswer.isNullOrBlank()
}

// ============================================================
// ProfileViewModel V2.0
// ============================================================

class ProfileViewModel(
    private val tokenManager: TokenManager
) : ViewModel() {

    // ========================================================
    // UI状态
    // ========================================================

    private val _uiState =
        MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)

    val uiState: StateFlow<ProfileUiState> =
        _uiState.asStateFlow()

    // ========================================================
    // 原始服务器数据
    // ========================================================

    private val _originalData =
        MutableStateFlow<UserProfileResponse?>(null)

    val originalData =
        _originalData.asStateFlow()

    // ========================================================
    // 编辑状态
    // ========================================================

    private val _isEditing =
        MutableStateFlow(false)

    val isEditing =
        _isEditing.asStateFlow()

    // ========================================================
    // 当前编辑数据
    // ========================================================

    private val _editedData =
        MutableStateFlow(EditedProfileData())

    val editedData =
        _editedData.asStateFlow()

    // ========================================================
    // 是否存在未保存修改
    // ========================================================

    private val _hasChanges =
        MutableStateFlow(false)

    val hasChanges =
        _hasChanges.asStateFlow()

    // ========================================================
    // Dialog状态
    // ========================================================

    private val _dialogState =
        MutableStateFlow<ProfileDialogState>(ProfileDialogState.None)

    val dialogState =
        _dialogState.asStateFlow()

    // ========================================================
    // 保存状态
    // ========================================================

    private val _isSaving =
        MutableStateFlow(false)

    val isSaving =
        _isSaving.asStateFlow()

    // ========================================================
    // 加载个人信息
    // ========================================================

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading

            val token = tokenManager.getToken()

            if (token.isNullOrBlank()) {
                _uiState.value = ProfileUiState.Error("登录状态失效，请重新登录")
                return@launch
            }

            try {
                val response = RetrofitClient
                    .authApi
                    .getProfile("Bearer $token")

                if (response.code == 200 && response.data != null) {
                    val data = response.data

                    _originalData.value = data

                    _editedData.value = EditedProfileData(
                        nickname = data.nickname ?: "",
                        // ❌ phone 已删除
                        age = data.age?.toString() ?: "",
                        gender = when (data.gender) {
                            1 -> "男"
                            0 -> "女"
                            else -> ""
                        },
                        emergencyPhone = data.emergencyPhone ?: "",
                        securityQuestion = data.securityQuestion ?: "",
                        securityAnswer = data.securityAnswer ?: ""
                    )

                    _uiState.value = ProfileUiState.Success(data)
                    _hasChanges.value = false
                } else {
                    _uiState.value = ProfileUiState.Error(
                        response.message ?: "获取个人信息失败"
                    )
                }
            } catch (e: HttpException) {
                val errorMsg = try {
                    val body = e.response()?.errorBody()?.string()
                    if (body != null) JSONObject(body).optString("message", "获取信息失败")
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

    // ============================================================
    // 进入编辑模式
    // ============================================================

    fun enterEditMode() {
        val data = _originalData.value

        if (data != null) {
            _editedData.value = EditedProfileData(
                nickname = data.nickname ?: "",
                // ❌ phone 已删除
                age = data.age?.toString() ?: "",
                gender = when (data.gender) {
                    1 -> "男"
                    0 -> "女"
                    else -> ""
                },
                emergencyPhone = data.emergencyPhone ?: "",
                securityQuestion = data.securityQuestion ?: "",
                securityAnswer = data.securityAnswer ?: ""
            )
            _hasChanges.value = false
        }

        _isEditing.value = true
    }

    // ============================================================
    // 退出编辑模式
    // ============================================================

    fun exitEditMode() {
        restoreEditData()
        _isEditing.value = false
        _hasChanges.value = false
        _dialogState.value = ProfileDialogState.None
    }

    // ============================================================
    // 恢复编辑数据
    // ============================================================

    private fun restoreEditData() {
        val data = _originalData.value ?: return

        _editedData.value = EditedProfileData(
            nickname = data.nickname ?: "",
            // ❌ phone 已删除
            age = data.age?.toString() ?: "",
            gender = when (data.gender) {
                1 -> "男"
                0 -> "女"
                else -> ""
            },
            emergencyPhone = data.emergencyPhone ?: "",
            securityQuestion = data.securityQuestion ?: "",
            securityAnswer = data.securityAnswer ?: ""
        )
    }

    // ============================================================
    // 更新编辑数据
    // ============================================================

    fun updateEditedData(block: EditedProfileData.() -> EditedProfileData) {
        val newData = _editedData.value.block()
        _editedData.value = newData
        checkHasChanges()
    }

    // ============================================================
    // 检测是否修改
    // ============================================================

    private fun checkHasChanges() {
        val original = _originalData.value ?: return
        val edited = _editedData.value

        val nicknameChanged =
            edited.nickname != (original.nickname ?: "")

        // ❌ phoneChanged 已删除

        val ageChanged =
            edited.age != (original.age?.toString() ?: "")

        val genderChanged =
            edited.gender != when (original.gender) {
                1 -> "男"
                0 -> "女"
                else -> ""
            }

        val emergencyChanged =
            edited.emergencyPhone != (original.emergencyPhone ?: "")

        val originalSecurityEmpty =
            original.securityQuestion.isNullOrBlank() &&
                    original.securityAnswer.isNullOrBlank()

        val editedSecurityComplete =
            edited.securityQuestion.isNotBlank() &&
                    edited.securityAnswer.isNotBlank()

        val securityChanged =
            originalSecurityEmpty && editedSecurityComplete

        _hasChanges.value =
            nicknameChanged ||
                    // ❌ phoneChanged 已删除
                    ageChanged ||
                    genderChanged ||
                    emergencyChanged ||
                    securityChanged
    }

    // ============================================================
    // 数据校验
    // ============================================================

    private fun validateProfile(): String? {
        val data = _editedData.value

        if (data.nickname.isBlank()) {
            return "昵称不能为空"
        }

        // ❌ 手机号校验已删除

        if (data.age.isNotBlank()) {
            val age = data.age.toIntOrNull()
            if (age == null || age < 0 || age > 120) {
                return "年龄格式错误"
            }
        }

        if (data.gender.isNotBlank() &&
            data.gender != "男" &&
            data.gender != "女"
        ) {
            return "性别只能填写男或女"
        }

        if (data.emergencyPhone.isNotBlank() &&
            !data.emergencyPhone.matches(Regex("^\\d+$"))
        ) {
            return "紧急联系人号码格式错误"
        }

        val original = _originalData.value
        val securityNotSet =
            original?.securityQuestion.isNullOrBlank() &&
                    original?.securityAnswer.isNullOrBlank()

        if (securityNotSet) {
            val question = data.securityQuestion
            val answer = data.securityAnswer

            if (question.isNotBlank() || answer.isNotBlank()) {
                if (question.isBlank() || answer.isBlank()) {
                    return "密保问题和答案必须同时填写"
                }
            }
        }

        return null
    }

    // ============================================================
    // 保存按钮点击
    // ============================================================

    fun saveProfile() {
        _dialogState.value = ProfileDialogState.SaveConfirm
    }

    // ============================================================
    // 确认保存
    // ============================================================

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
                _isSaving.value = false
                return@launch
            }

            try {
                val edited = _editedData.value
                val original = _originalData.value

                val request = UpdateProfileRequest(
                    nickname = if (edited.nickname != (original?.nickname ?: "")) edited.nickname else null,
                    // ❌ phone 不再传递
                    age = edited.age.toIntOrNull(),
                    gender = when (edited.gender) {
                        "男" -> 1
                        "女" -> 0
                        else -> null
                    },
                    emergencyPhone = if (edited.emergencyPhone != (original?.emergencyPhone ?: "")) edited.emergencyPhone else null,
                    securityQuestion = if (edited.securityQuestion.isNotBlank()) edited.securityQuestion else null,
                    securityAnswer = if (edited.securityAnswer.isNotBlank()) edited.securityAnswer else null
                )

                // 检查是否有实际修改
                if (request.nickname == null &&
                    // ❌ request.phone 已删除
                    request.age == null &&
                    request.gender == null &&
                    request.emergencyPhone == null
                ) {
                    // 检查是否有密保修改
                    val originalSecurityEmpty =
                        original?.securityQuestion.isNullOrBlank() &&
                                original?.securityAnswer.isNullOrBlank()

                    val editedSecurityComplete =
                        edited.securityQuestion.isNotBlank() &&
                                edited.securityAnswer.isNotBlank()

                    if (!(originalSecurityEmpty && editedSecurityComplete)) {
                        Toast.makeText(context, "没有需要修改的信息", Toast.LENGTH_SHORT).show()
                        _isEditing.value = false
                        _isSaving.value = false
                        return@launch
                    }
                }

                val response = RetrofitClient
                    .authApi
                    .updateProfile("Bearer $token", request)

                if (response.code == 200) {
                    Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                    loadProfile()
                    _isEditing.value = false
                    onSuccess()
                } else {
                    Toast.makeText(
                        context,
                        response.message ?: "保存失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: HttpException) {
                val errorMsg = try {
                    val body = e.response()?.errorBody()?.string()
                    if (body != null) JSONObject(body).optString("message", "请求失败")
                    else "请求失败"
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

    // ============================================================
    // Dialog 控制
    // ============================================================

    fun cancelSave() {
        _dialogState.value = ProfileDialogState.None
    }

    fun showDiscardDialog() {
        _dialogState.value = ProfileDialogState.DiscardConfirm
    }

    fun confirmDiscard() {
        restoreEditData()
        _hasChanges.value = false
        _dialogState.value = ProfileDialogState.None
        _isEditing.value = false
    }

    fun cancelDiscard() {
        _dialogState.value = ProfileDialogState.None
    }

    fun showSecurityReminder() {
        _dialogState.value = ProfileDialogState.SecurityReminder
    }

    fun confirmSecurityReminder() {
        _dialogState.value = ProfileDialogState.None
        enterEditMode()

        val current = _editedData.value

        _editedData.value = current.copy(
            securityQuestion = "",
            securityAnswer = ""
        )

        _hasChanges.value = false
    }

    fun cancelSecurityReminder() {
        _dialogState.value = ProfileDialogState.None
    }

    fun resetDialog() {
        _dialogState.value = ProfileDialogState.None
    }

    // ============================================================
    // 退出登录
    // ============================================================

    suspend fun logout() {
        tokenManager.clear()
    }

    // ============================================================
    // ViewModel销毁
    // ============================================================

    override fun onCleared() {
        super.onCleared()
    }
}