package com.example.chitu.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chitu.R
import com.example.chitu.data.local.TokenManager
import com.example.chitu.data.remote.dto.UserProfileResponse
import com.example.chitu.viewmodel.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.ScrollState

// ============================================================
// 赤兔 UI Design System
// ============================================================

private val ChituRed = Color(0xFFC62828)
private val PageBackground = Color(0xFFFAFAFA)
private val TextPrimary = Color(0xFF212121)
private val TextSecondary = Color(0xFF757575)

// ============================================================
// ProfileScreen V2.0
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()

    val viewModel: ProfileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(tokenManager) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val editedData by viewModel.editedData.collectAsState()
    val hasChanges by viewModel.hasChanges.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    fun handleBack() {
        if (isEditing) {
            if (hasChanges) {
                viewModel.showDiscardDialog()
            } else {
                viewModel.exitEditMode()
            }
        } else {
            navController.popBackStack()
        }
    }

    BackHandler {
        handleBack()
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = PageBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "编辑个人信息" else "个人信息",
                        color = ChituRed,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = ChituRed
                        )
                    }
                },
                actions = {
                    if (isEditing) {
                        TextButton(
                            onClick = {
                                if (hasChanges) {
                                    viewModel.showDiscardDialog()
                                } else {
                                    viewModel.exitEditMode()
                                }
                            }
                        ) {
                            Text(
                                text = "取消",
                                color = ChituRed,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        IconButton(onClick = { viewModel.enterEditMode() }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑",
                                tint = ChituRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PageBackground
                )
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ChituRed)
                }
            }

            is ProfileUiState.Success -> {
                ProfileContent(
                    data = state.data,
                    isEditing = isEditing,
                    editedData = editedData,
                    onEditDataChange = { viewModel.updateEditedData(it) },
                    onSaveClick = { viewModel.saveProfile() },
                    onSecurityClick = { viewModel.showSecurityReminder() },
                    modifier = Modifier.padding(padding)
                )
            }

            is ProfileUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = Color.Red
                    )
                }
            }
        }
    }

    ProfileDialogs(
        dialogState = dialogState,
        viewModel = viewModel,
        context = context
    )
}

// ============================================================
// ProfileDialogs
// ============================================================

@Composable
fun ProfileDialogs(
    dialogState: ProfileDialogState,
    viewModel: ProfileViewModel,
    context: android.content.Context
) {
    when (dialogState) {
        ProfileDialogState.SaveConfirm -> {
            AlertDialog(
                onDismissRequest = { viewModel.cancelSave() },
                title = {
                    Text(
                        text = "保存修改",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(text = "是否保存当前修改的信息？")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.confirmSave(context) {
                                viewModel.exitEditMode()
                            }
                        }
                    ) {
                        Text(
                            text = "保存",
                            color = ChituRed
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelSave() }) {
                        Text(
                            text = "取消",
                            color = TextSecondary
                        )
                    }
                }
            )
        }

        ProfileDialogState.DiscardConfirm -> {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDiscard() },
                title = {
                    Text(
                        text = "放弃修改？",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(text = "当前信息已经修改，是否保存？")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.confirmSave(context) {
                                viewModel.exitEditMode()
                            }
                        }
                    ) {
                        Text(
                            text = "保存",
                            color = ChituRed
                        )
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { viewModel.confirmDiscard() }) {
                            Text(
                                text = "不保存",
                                color = ChituRed
                            )
                        }
                        TextButton(onClick = { viewModel.cancelDiscard() }) {
                            Text(
                                text = "取消",
                                color = TextSecondary
                            )
                        }
                    }
                }
            )
        }

        ProfileDialogState.SecurityReminder -> {
            AlertDialog(
                onDismissRequest = { viewModel.cancelSecurityReminder() },
                title = {
                    Text(
                        text = "安全提醒",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "密保问题和答案只能修改一次，" +
                                "请牢记您的密保答案，" +
                                "用于后续找回密码和修改密码。"
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmSecurityReminder() }) {
                        Text(
                            text = "确认填写",
                            color = ChituRed
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelSecurityReminder() }) {
                        Text(
                            text = "取消",
                            color = TextSecondary
                        )
                    }
                }
            )
        }

        ProfileDialogState.None -> {}
    }
}

// ============================================================
// ProfileContent（精修版）
// 删除：头像下方昵称、手机号行
// ============================================================

@Composable
fun ProfileContent(
    data: UserProfileResponse,
    isEditing: Boolean,
    editedData: EditedProfileData,
    onEditDataChange: (EditedProfileData.() -> EditedProfileData) -> Unit,
    onSaveClick: () -> Unit,
    onSecurityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val securityAlreadySet = data.isSecuritySet()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 头像
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(ChituRed),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_avatar),
                contentDescription = "头像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ❌ 头像下方昵称已删除

        Spacer(modifier = Modifier.height(16.dp))

        // ====================================================
        // 卡片区域
        // ====================================================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    // 昵称（卡片内）
                    EditInfoRow(
                        label = "昵称",
                        value = if (isEditing) editedData.nickname else (data.nickname?.takeIf { it.isNotBlank() } ?: "未填写"),
                        isEditing = isEditing,
                        onValueChange = { onEditDataChange { copy(nickname = it) } },
                        coroutineScope = coroutineScope,
                        scrollState = scrollState
                    )
                    Divider()

                    // ❌ 手机号行已删除

                    // 年龄
                    EditInfoRow(
                        label = "年龄",
                        value = if (isEditing) editedData.age else (data.age?.toString() ?: "未填写"),
                        isEditing = isEditing,
                        keyboardType = KeyboardType.Number,
                        onValueChange = {
                            if (it.isEmpty() || it.all { c -> c.isDigit() }) {
                                onEditDataChange { copy(age = it) }
                            }
                        },
                        coroutineScope = coroutineScope,
                        scrollState = scrollState
                    )
                    Divider()

                    // 性别
                    EditInfoRow(
                        label = "性别",
                        value = if (isEditing) editedData.gender else when (data.gender) { 1 -> "男"; 0 -> "女"; else -> "未填写" },
                        isEditing = isEditing,
                        onValueChange = { onEditDataChange { copy(gender = it) } },
                        coroutineScope = coroutineScope,
                        scrollState = scrollState
                    )
                    Divider()

                    // 紧急联系人
                    EditInfoRow(
                        label = "紧急联系人",
                        value = if (isEditing) editedData.emergencyPhone else (data.emergencyPhone?.takeIf { it.isNotBlank() } ?: "未填写"),
                        isEditing = isEditing,
                        keyboardType = KeyboardType.Phone,
                        onValueChange = { onEditDataChange { copy(emergencyPhone = it) } },
                        coroutineScope = coroutineScope,
                        scrollState = scrollState
                    )

                    // 密保
                    if (!securityAlreadySet) {
                        Divider()
                        SecuritySection(
                            isEditing = isEditing,
                            editedData = editedData,
                            onEditDataChange = onEditDataChange,
                            onSecurityClick = onSecurityClick,
                            coroutineScope = coroutineScope,
                            scrollState = scrollState
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 底部按钮（编辑模式显示）
        if (isEditing) {
            Button(
                onClick = onSaveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ChituRed)
            ) {
                Text(
                    text = "保存信息",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ============================================================
// SecuritySection（不变）
// ============================================================

@Composable
fun SecuritySection(
    isEditing: Boolean,
    editedData: EditedProfileData,
    onEditDataChange: (EditedProfileData.() -> EditedProfileData) -> Unit,
    onSecurityClick: () -> Unit,
    coroutineScope: CoroutineScope,
    scrollState: ScrollState
) {
    if (isEditing) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "密保设置",
                color = ChituRed,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            EditInfoRow(
                label = "密保问题",
                value = editedData.securityQuestion,
                isEditing = true,
                onValueChange = {
                    onEditDataChange { copy(securityQuestion = it) }
                },
                coroutineScope = coroutineScope,
                scrollState = scrollState
            )

            EditInfoRow(
                label = "密保答案",
                value = editedData.securityAnswer,
                isEditing = true,
                onValueChange = {
                    onEditDataChange { copy(securityAnswer = it) }
                },
                coroutineScope = coroutineScope,
                scrollState = scrollState
            )

            Text(
                text = "⚠️ 密保设置后不可修改，请牢记您的答案",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSecurityClick() }
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "密保",
                fontSize = 15.sp,
                color = TextSecondary
            )
            Text(
                text = "未填写",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ChituRed
            )
        }
    }
}

// ============================================================
// EditInfoRow（精修版）
// ============================================================

@Composable
fun EditInfoRow(
    label: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    coroutineScope: CoroutineScope,
    scrollState: ScrollState
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = TextSecondary,
            modifier = Modifier.width(90.dp)
        )

        if (isEditing) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    textAlign = TextAlign.End
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                cursorBrush = SolidColor(ChituRed),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = "请输入",
                                fontSize = 15.sp,
                                color = Color(0xFFAAAAAA),
                                textAlign = TextAlign.End
                            )
                        }
                        innerTextField()
                    }
                }
            )
        } else {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}