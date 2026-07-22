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
    val uiState by profileViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
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
                        Toast.makeText(context, "开发中", Toast.LENGTH_SHORT).show()
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
                Spacer(modifier = Modifier.height(40.dp))

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

                Spacer(modifier = Modifier.height(60.dp))

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
                                drivingViewModel.startDriving()
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
                        formatTime = drivingViewModel::formatTime
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
    formatTime: (Int) -> String
) {
    // 4 小时疲劳驾驶阈值
    val fatigueLimit = 4 * 60 * 60
    val progress = (elapsedSeconds.toFloat() / fatigueLimit).coerceIn(0f, 1f)
    val remain = (fatigueLimit - elapsedSeconds).coerceAtLeast(0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🚛 驾驶中",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ChituRed
            )

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

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFFFF9800),
                trackColor = Color(0xFFEAEAEA)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "疲劳驾驶进度 ${(progress * 100).toInt()}%",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                Text(
                    text = "剩余 ${formatTime(remain)}",
                    color = ChituRed,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}