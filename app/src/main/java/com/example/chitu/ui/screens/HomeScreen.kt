package com.example.chitu.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chitu.R
import com.example.chitu.data.local.TokenManager
import com.example.chitu.viewmodel.DrivingViewModel
import com.example.chitu.viewmodel.ProfileUiState
import com.example.chitu.viewmodel.ProfileViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape


private val ChituRed = Color(0xFFC62828)
private val PageBackground = Color(0xFFFAFAFA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onNavigateToProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val drivingViewModel: DrivingViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return DrivingViewModel(context) as T
            }
        }
    )

    val isDriving by drivingViewModel.isDriving.collectAsState()
    val elapsedSeconds by drivingViewModel.elapsedSeconds.collectAsState()



    val profileViewModel: ProfileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(tokenManager) as T
            }
        }
    )
    // ✅ 使用 collectAsState() 监听 UserSetting 变化（非空默认值）
    val setting by profileViewModel.settingState.collectAsState()
    val reminderInterval = setting.reminderInterval

    val uiState by profileViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        profileViewModel.loadSetting()
        profileViewModel.loadProfile()
        drivingViewModel.checkAndRestoreDriving()
    }

    // ==================== 侧边栏 ====================
    val drawerContent = @Composable {
        ModalDrawerSheet(
            modifier = Modifier.fillMaxWidth(0.85f),
            drawerContainerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp)
            ) {
                when (uiState) {
                    is ProfileUiState.Success -> {
                        val data = (uiState as ProfileUiState.Success).data
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(ChituRed)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_avatar),
                                contentDescription = "头像",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = data.nickname ?: "未设置昵称",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                        Text(
                            text = data.phone,
                            fontSize = 14.sp,
                            color = Color(0xFF757575)
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.Gray)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("加载中...", color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray)
                Spacer(modifier = Modifier.height(16.dp))

                DrawerMenuItem(
                    text = "个人信息",
                    onClick = {
                        scope.launch {
                            drawerState.animateTo(
                                targetValue = DrawerValue.Closed,
                                anim = tween(durationMillis = 350)
                            )
                            delay(50)
                            onNavigateToProfile()
                        }
                    }
                )
                DrawerMenuItem(
                    text = "行程日志",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("trip_list")
                    }
                )
                DrawerMenuItem(
                    text = "驾驶统计",
                    onClick = {
                        scope.launch { drawerState.close() }
                        Toast.makeText(context, "开发中", Toast.LENGTH_SHORT).show()
                    }
                )
                DrawerMenuItem(
                    text = "系统设置",
                    onClick = {
                        scope.launch { drawerState.close() }
                        Toast.makeText(context, "开发中", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                DrawerMenuItem(
                    text = "退出登录",
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            tokenManager.clear()
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    },
                    isBottom = true
                )
            }
        }
    }

    // ==================== 主界面 ====================
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = drawerContent,
        gesturesEnabled = true,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isOpen) drawerState.close()
                                    else drawerState.open()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "菜单",
                                tint = ChituRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PageBackground
                    )
                )
            },
            containerColor = PageBackground
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(PageBackground)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(15.dp))

                Text(
                    text = "赤兔",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChituRed
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "货运千里，赤兔随行",
                    fontSize = 16.sp,
                    color = Color(0xFF757575)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ====================================================
                // 按钮区域（完全不变）
                // ====================================================
                if (isDriving) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFFA500), Color(0xFFFF8C00))
                                    )
                                )
                                .clickable {
                                    drivingViewModel.stopDriving()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Stop,
                                    contentDescription = "结束驾驶",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "结束驾驶",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(ChituRed, ChituRed.copy(alpha = 0.7f))
                                )
                            )
                            .clickable {
                                drivingViewModel.startDriving(reminderInterval)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "开始驾驶",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "开始驾驶",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // ====================================================
                // ✅ 底部驾驶状态卡片（仅驾驶中显示）
                // ====================================================
                Spacer(modifier = Modifier.weight(1f))

                if (isDriving) {
                    DrivingStatusCard(
                        elapsedSeconds = elapsedSeconds,
                        formatTime = drivingViewModel::formatTime,
                        fatigueLimitMinutes = reminderInterval   // ✅ 传递用户自定义阈值
                    )
                }
            }
        }
    }
}

// ===== 侧边栏菜单项 =====
@Composable
fun DrawerMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isBottom: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isBottom) ChituRed else Color(0xFF212121)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = if (isBottom) FontWeight.Normal else FontWeight.Medium,
            color = if (isBottom) ChituRed else Color(0xFF212121)
        )
    }
}

// ============================================================
// ✅ DrivingStatusCard：驾驶状态卡片（底部显示）
// ============================================================

@Composable
fun DrivingStatusCard(
    elapsedSeconds: Int,
    formatTime: (Int) -> String,
    fatigueLimitMinutes: Int = 240   // ✅ 新增参数
) {
    // ✅ 使用用户自定义阈值（分钟 → 秒）
    val fatigueLimit = fatigueLimitMinutes * 60
    val progress = (elapsedSeconds.toFloat() / fatigueLimit).coerceIn(0f, 1f)
    val remain = (fatigueLimit - elapsedSeconds).coerceAtLeast(0)

    // ✅ 动态阈值：基于用户设置的疲劳提醒时间
    //    normalLimit = 50%  正常驾驶区间
    //    warningLimit = 75%  注意休息区间
    val normalLimit = (fatigueLimit * 0.5).toInt()
    val warningLimit = (fatigueLimit * 0.75).toInt()

    // ✅ 驾驶状态文字 + 颜色（完全基于用户自定义阈值）
    val (statusText, statusColor) = when {
        elapsedSeconds < normalLimit ->
            "正常驾驶" to Color(0xFF2E7D32)

        elapsedSeconds < warningLimit ->
            "注意休息" to Color(0xFFF9A825)

        elapsedSeconds < fatigueLimit ->
            "即将疲劳驾驶" to Color(0xFFFB8C00)

        else ->
            "请立即停车休息" to Color(0xFFD32F2F)
    }

    // ✅ 预计提醒时间
    val reminderTime = remember(remain) {
        java.time.LocalTime.now()
            .plusSeconds(remain.toLong())
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(3.dp)  // ✅ 阴影降低
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ✅ 状态文字（动态颜色）
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = statusText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = formatTime(elapsedSeconds),
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = ChituRed
            )

            Text(
                text = "已驾驶时长",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ✅ 胶囊进度条（颜色随状态变化）
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = statusColor,
                trackColor = Color(0xFFEAEAEA)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ✅ 底部左右信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧：疲劳驾驶风险
                Column {
                    Text(
                        text = "疲劳驾驶风险",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                // 右侧：预计提醒时间
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "预计提醒时间",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = reminderTime,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
        }
    }
}