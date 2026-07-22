package com.example.chitu.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chitu.data.local.TokenManager
import kotlinx.coroutines.delay

@Preview
@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit = {},  // ✅ 修改：跳转到登录页
    onNavigateToHome: () -> Unit = {}     // ✅ 新增：跳转到主页
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    // ✅ 核心逻辑：延迟 1 秒后检查 Token
    LaunchedEffect(Unit) {
        delay(1500) // 保留你原来的 1 秒延迟（我改成 1.5 秒，让启动页看得更清楚）

        // 读取 Token
        val token = tokenManager.getToken()

        if (!token.isNullOrEmpty()) {
            // 有 Token → 直接进主页
            onNavigateToHome()
        } else {
            // 无 Token → 去登录页
            onNavigateToLogin()
        }
    }

    // ⬇️ 下面是你原来的 UI，完全没变
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "货\n\n\n运\n\n\n千\n\n\n里",
                    color = Color.Black,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .wrapContentHeight()
                        .width(IntrinsicSize.Min)
                        .padding(horizontal = 30.dp)
                )
                Text(
                    text = "赤\n\n\n兔\n\n\n随\n\n\n行",
                    color = Color.Black,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .wrapContentHeight()
                        .width(IntrinsicSize.Min)
                        .padding(horizontal = 30.dp)
                )
            }
        }
    }
}