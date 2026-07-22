package com.example.chitu.ui.screens

import android.R.attr.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.VisualTransformation

import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chitu.viewmodel.LoginUiState
import com.example.chitu.viewmodel.LoginViewModel
import androidx.compose.ui.platform.LocalContext  // 用于 Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.chitu.data.local.TokenManager

@Preview
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    toRegister: () -> Unit = {}
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isInputValid = phone.isNotEmpty() && password.isNotEmpty()

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    val viewModel: LoginViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel(tokenManager) as T
            }
        }
    )

    val loginState by viewModel.loginState.collectAsState()

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

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("手机号：", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black.copy(alpha = 0.6f),
                cursorColor = Color.Black.copy(alpha = 0.6f),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black.copy(alpha = 0.6f)
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码:", color = Color.Gray) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black.copy(alpha = 0.6f),
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
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.login(phone, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = isInputValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isInputValid) Color.Red.copy(alpha = 0.7f) else Color(0xFFD32F2F).copy(alpha = 0.7f),
                disabledContainerColor = Color(0xFFD32F2F).copy(alpha = 0.7f)
            )
        ) {
            Text("登录", color = Color.Black)
        }

        // 监听登录状态，处理成功和错误
        LaunchedEffect(loginState) {
            when (loginState) {
                is LoginUiState.Success -> {
                    // 登录成功，跳转主页
                    onLoginSuccess()
                }
                is LoginUiState.Error -> {
                    // 显示错误信息（Toast）
                    val errorMsg = (loginState as LoginUiState.Error).message
                    android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                    // 重置状态，避免反复弹框
                    viewModel.resetState()
                }
                else -> {}
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        TextButton(
            onClick = { toRegister() },
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Text("没有账号？去注册", color = Color.Black)
        }
    }
}