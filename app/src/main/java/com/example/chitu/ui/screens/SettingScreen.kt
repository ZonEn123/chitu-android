package com.example.chitu.ui.screens

import androidx.activity.ComponentActivity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chitu.data.local.TokenManager
import com.example.chitu.viewmodel.SettingViewModel
import kotlin.math.roundToInt

private val ChituRed = ComposeColor(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    navController: NavController,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    // 使用 Activity 级别的 ViewModelStoreOwner，与 MainActivity 共享实例
    val activity = context as? ComponentActivity
    checkNotNull(activity) { "SettingScreen 必须在 ComponentActivity 中使用" }
    val viewModel: SettingViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return SettingViewModel(activity.applicationContext, tokenManager) as T
            }
        }
    )

    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    // 加载设置
    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    // 监听保存状态
    LaunchedEffect(saveState) {
        when (saveState) {
            is SettingViewModel.SaveState.Success -> {
                Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
            }
            is SettingViewModel.SaveState.Error -> {
                Toast.makeText(context, (saveState as SettingViewModel.SaveState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "系统设置",
                        color = ChituRed,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = ChituRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ChituRed)
            }
            return@Scaffold
        }

        val currentSettings = settings ?: return@Scaffold

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 顶部间距
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ===== 显示设置 =====
            item {
                SettingGroupHeader(title = "显示设置")
            }

            item {
                SettingSwitchItem(
                    title = "深色模式",
                    subtitle = "切换应用主题颜色",
                    checked = currentSettings.darkMode == 1,
                    onCheckedChange = { checked ->
                        val value = if (checked) 1 else 0
                        viewModel.saveSetting(darkMode = value)
                    }
                )
            }

            // ===== 提醒设置 =====
            item {
                SettingGroupHeader(title = "提醒设置")
            }

            item {
                SettingSwitchItem(
                    title = "声音提醒",
                    subtitle = "驾驶提醒时播放声音",
                    checked = currentSettings.soundEnabled == 1,
                    onCheckedChange = { checked ->
                        val value = if (checked) 1 else 0
                        viewModel.saveSetting(soundEnabled = value)
                    }
                )
            }

            item {
                SettingSwitchItem(
                    title = "震动提醒",
                    subtitle = "驾驶提醒时震动手机",
                    checked = currentSettings.vibrationEnabled == 1,
                    onCheckedChange = { checked ->
                        val value = if (checked) 1 else 0
                        viewModel.saveSetting(vibrationEnabled = value)
                    }
                )
            }

            // ✅ 优化后的滑块（无抖动 + HH:mm 格式）
            item {
                SettingSliderItem(
                    title = "疲劳提醒时间",
                    subtitle = "连续驾驶达到该时间后触发提醒",
                    value = currentSettings.reminderInterval,
                    onValueChange = { newValue ->
                        viewModel.saveSetting(reminderInterval = newValue)
                    }
                )
            }

            // ===== 安全设置 =====
            item {
                SettingGroupHeader(title = "安全设置")
            }
            item {
                SettingClickItem(
                    title = "修改密码",
                    onClick = { navController.navigate("security_setting") }
                )
            }

            // ===== 底部留白 =====
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ============================================================
// 组件
// ============================================================

@Composable
fun SettingGroupHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = ChituRed,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ChituRed,
                    checkedTrackColor = ChituRed.copy(alpha = 0.5f)
                )
            )
        }
    }
}

// ✅ 优化后的滑块组件（无抖动 + HH:mm 格式）
@Composable
fun SettingSliderItem(
    title: String,
    subtitle: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    var tempValue by remember(value) {
        mutableStateOf(value.toFloat())
    }

    // ✅ 使用 derivedStateOf 计算对齐后的值，避免浮点精度问题
    val snappedValue by derivedStateOf {
        ((tempValue / 30f).roundToInt() * 30).coerceIn(60, 480)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ✅ 固定尺寸的时间显示，避免布局抖动
                Surface(
                    modifier = Modifier
                        .width(110.dp)
                        .height(42.dp),
                    shape = RoundedCornerShape(21.dp),
                    color = ChituRed.copy(alpha = 0.12f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatTime(snappedValue),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChituRed,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Slider(
                value = tempValue,
                onValueChange = { newValue ->
                    tempValue = newValue
                },
                onValueChangeFinished = {
                    // ✅ 保存对齐后的值
                    onValueChange(snappedValue)
                },
                valueRange = 60f..480f,
                steps = 13,
                colors = SliderDefaults.colors(
                    thumbColor = ChituRed,
                    activeTrackColor = ChituRed,
                    inactiveTrackColor = ChituRed.copy(alpha = 0.25f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "1小时",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "当前 ${formatTime(snappedValue)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChituRed
                )
                Text(
                    text = "8小时",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ✅ 时间格式化：HH:mm（车机风格，固定宽度）
fun formatTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return String.format("%02d:%02d", hours, mins)
}
/** 可点击的菜单项（用于"修改密码"等入口） */
@Composable
fun SettingClickItem(
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(">", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
