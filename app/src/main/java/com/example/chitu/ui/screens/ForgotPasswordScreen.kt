package com.example.chitu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chitu.viewmodel.ForgotStep
import com.example.chitu.viewmodel.SecurityUiState
import com.example.chitu.viewmodel.SecurityViewModel

private val ChituRed = Color(0xFFC62828)
private val BgColor = Color(0xFFFAFAFA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: SecurityViewModel = viewModel()
) {
    val step by viewModel.forgotStep.collectAsState()
    val state by viewModel.uiState.collectAsState()

    var phone by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is SecurityUiState.Success) {
            navController.navigate("login") {
                popUpTo("login") { inclusive = true }
            }
            viewModel.reset()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("找回密码", fontWeight = FontWeight.Bold, color = Color(0xFF212121)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack(); viewModel.reset() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = ChituRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
            )
        },
        containerColor = BgColor
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // ===== 步骤 1：输入手机号 =====
            if (step is ForgotStep.InputPhone || step is ForgotStep.VerifySecurity || step is ForgotStep.ResetPassword) {
                Text("手机号", fontSize = 13.sp, color = Color(0xFF888888))
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.take(11) },
                        placeholder = { Text("请输入注册手机号") },
                        singleLine = true,
                        enabled = step is ForgotStep.InputPhone,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChituRed,
                            unfocusedBorderColor = Color(0xFFDDDDDD)
                        )
                    )
                    if (step !is ForgotStep.InputPhone && phone.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text("✓", color = Color(0xFF4CAF50), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ===== 步骤 2：安全问题 =====
            if (step is ForgotStep.VerifySecurity || step is ForgotStep.ResetPassword) {
                val q = (step as? ForgotStep.VerifySecurity)?.question ?: ""
                Text("安全问题", fontSize = 13.sp, color = Color(0xFF888888))
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF5F5F5),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(q, modifier = Modifier.padding(16.dp), fontSize = 15.sp, color = Color(0xFF333333))
                }
                Spacer(Modifier.height(12.dp))

                if (step is ForgotStep.VerifySecurity) {
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        placeholder = { Text("输入答案") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChituRed,
                            unfocusedBorderColor = Color(0xFFDDDDDD)
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val p = (step as ForgotStep.VerifySecurity).phone
                            viewModel.verifySecurity(p, answer)
                        },
                        enabled = state !is SecurityUiState.Loading,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChituRed)
                    ) { Text("验证", color = Color.White, fontSize = 16.sp) }
                    Spacer(Modifier.height(16.dp))
                }
                if (step !is ForgotStep.VerifySecurity && answer.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("答案正确 ✓", color = Color(0xFF4CAF50), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ===== 步骤 3：设置新密码 =====
            if (step is ForgotStep.ResetPassword) {
                val p = (step as ForgotStep.ResetPassword).phone
                Text("设置新密码", fontSize = 13.sp, color = Color(0xFF888888))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = newPwd,
                    onValueChange = { newPwd = it },
                    placeholder = { Text("新密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChituRed,
                        unfocusedBorderColor = Color(0xFFDDDDDD)
                    )
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPwd,
                    onValueChange = { confirmPwd = it },
                    placeholder = { Text("确认密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChituRed,
                        unfocusedBorderColor = Color(0xFFDDDDDD)
                    )
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.resetPassword(p, newPwd, confirmPwd) },
                    enabled = state !is SecurityUiState.Loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChituRed)
                ) { Text("完成", color = Color.White, fontSize = 16.sp) }
            }

            // ===== 步骤 1 的"下一步"按钮 =====
            if (step is ForgotStep.InputPhone) {
                Button(
                    onClick = { viewModel.getSecurityQuestion(phone) },
                    enabled = state !is SecurityUiState.Loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChituRed)
                ) { Text("下一步", color = Color.White, fontSize = 16.sp) }
            }

            // ===== 错误提示 =====
            if (state is SecurityUiState.Error) {
                Spacer(Modifier.height(16.dp))
                Text(
                    (state as SecurityUiState.Error).message,
                    color = ChituRed, fontSize = 14.sp
                )
            }

            if (state is SecurityUiState.Loading) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ChituRed)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
