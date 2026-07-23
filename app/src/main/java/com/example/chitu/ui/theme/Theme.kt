package com.example.chitu.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.chitu.data.local.TokenManager

// ✅ 品牌色定义
private val ChituRed = Color(0xFFC62828)

@Composable
fun ChituTheme(
    darkMode: Int? = null,  // 0-浅色 1-深色 null-跟随系统
    content: @Composable () -> Unit
) {
    // 确定是否使用深色模式
    val isDark = when (darkMode) {
        1 -> true
        0 -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = ChituRed,
            secondary = ChituRed,
            tertiary = ChituRed,
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = ChituRed,
            secondary = ChituRed,
            tertiary = ChituRed,
            background = Color(0xFFFAFAFA),
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = Color(0xFF212121),
            onSurface = Color(0xFF212121)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}