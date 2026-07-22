package com.example.chitu.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff


import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chitu.viewmodel.RegisterUiState
import com.example.chitu.viewmodel.RegisterViewModel


@Preview
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit = {},
    toLogin: () -> Unit = {}
) {
    // 获取 Context（用于 Toast）
    val context = LocalContext.current

    // 状态变量
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // 三个框是否非空
    val isInputValid = phone.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()
    // 密码是否不一致
    val passwordMismatch = password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword

    val viewModel: RegisterViewModel = viewModel()
    val registerState by viewModel.registerState.collectAsState()


    LaunchedEffect(registerState) {
        when (registerState) {
            is RegisterUiState.Success -> {
                Toast.makeText(context, "注册成功，请登录", Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                // 跳转到登录页
                onRegisterSuccess()
            }
            is RegisterUiState.Error -> {
                Toast.makeText(context, (registerState as RegisterUiState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(110.dp))
        Text("Truckmate", fontSize = 30.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text("赤  兔", fontSize = 30.sp, color = Color.Red)
        Spacer(modifier = Modifier.height(60.dp))

        // 手机号输入框
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("手机号:", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black.copy(alpha = 0.6f),
                unfocusedBorderColor = Color.Black.copy(alpha = 0.6f),
                cursorColor = Color.Black.copy(alpha = 0.6f),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black.copy(alpha = 0.6f)
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 密码输入框
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码:", color = Color.Gray) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black.copy(alpha = 0.6f),
                unfocusedBorderColor = Color.Black.copy(alpha = 0.6f),
                cursorColor = Color.Black.copy(alpha = 0.6f),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black.copy(alpha = 0.6f)
            ),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = image,
                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                        tint = Color.Black.copy(alpha = 0.6f)
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 确认密码输入框
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("确认密码:", color = Color.Gray) },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black.copy(alpha = 0.6f),
                unfocusedBorderColor = Color.Black.copy(alpha = 0.6f),
                cursorColor = Color.Black.copy(alpha = 0.6f),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black.copy(alpha = 0.6f)
            ),
            trailingIcon = {
                val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = image,
                        contentDescription = if (confirmPasswordVisible) "隐藏密码" else "显示密码",
                        tint = Color.Black.copy(alpha = 0.6f)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))  // 固定间距，不再动态调整

        // 注册按钮（点击时校验密码一致性）
        Button(
            onClick = {
                // 如果密码不一致，弹 Toast 阻止提交
                if (passwordMismatch) {
                    Toast.makeText(context, "前后密码不一致", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                // 调用 ViewModel 进行注册（昵称不传，使用 null）
                viewModel.register(phone, password, nickname = null)
            },
            modifier = Modifier.fillMaxWidth(),
            //enabled = isInputValid && !passwordMismatch,  // 密码不一致时禁用按钮（更友好）
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isInputValid && !passwordMismatch) {
                    Color.Red.copy(alpha = 0.7f)
                } else {
                    Color(0xFFD32F2F).copy(alpha = 0.7f)
                },
                //disabledContainerColor = Color(0xFFD32F2F).copy(alpha = 0.7f)
            )
        ) {
            Text("注册", color = Color.Black)
        }

        Spacer(modifier = Modifier.weight(1f))
        TextButton(
            onClick = { toLogin() },
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Text("已有账号？去登录", color = Color.Black)
        }
    }
}