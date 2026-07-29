package com.example.chitu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chitu.data.local.TokenManager
import com.example.chitu.ui.screens.*
import com.example.chitu.ui.theme.ChituTheme
import com.example.chitu.viewmodel.SettingViewModel

class MainActivity : ComponentActivity() {

    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // 使用 Activity 级别的 SettingViewModel（与 SettingScreen 共享）
            val tokenManager = remember { TokenManager(applicationContext) }
            val settingViewModel: SettingViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return SettingViewModel(applicationContext, tokenManager) as T
                    }
                }
            )

            val settings by settingViewModel.settings.collectAsState()
            val darkMode = settings?.darkMode ?: 0

            // 监听 Token 变化，切换用户时重新加载设置
            val token by tokenManager.getTokenFlow().collectAsState(initial = null)
            LaunchedEffect(token) {
                if (token != null) {
                    settingViewModel.loadSettings()
                }
            }

            ChituTheme(darkMode = darkMode) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "splash") {

                    // 注册开屏页面
                    composable("splash") {
                        SplashScreen(
                            onNavigateToLogin = {
                                navController.navigate("login") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            },
                            onNavigateToHome = {
                                navController.navigate("home") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        )
                    }

                    // 注册登录页面
                    composable("login") {
                        LoginScreen(
                            navController = navController,
                            onLoginSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            toRegister = {
                                navController.navigate("register") {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            }
                        )
                    }

                    // 注册注册页面
                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = {
                                navController.navigate("login") {
                                    popUpTo("register") { inclusive = true }
                                }
                            },
                            toLogin = {
                                navController.navigate("login") {
                                    popUpTo("register") { inclusive = true }
                                }
                            }
                        )
                    }

                    // 安全模块：忘记密码流程
                    composable("forgot_password") {
                        ForgotPasswordScreen(navController = navController)
                    }
                    composable("security_setting") {
                        SecuritySettingScreen(navController = navController)
                    }

                    // 注册首页页面
                    composable("home") {
                        HomeScreen(
                            navController = navController,
                            onNavigateToProfile = {
                                navController.navigate("profile")
                            }
                        )
                    }

                    // 注册个人中心页面
                    composable("profile") {
                        ProfileScreen(
                            navController = navController
                        )
                    }

                    // 行程列表
                    composable("trip_list") {
                        TripListScreen(
                            navController = navController,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // 行程详情页（带参数 tripId）
                    composable(
                        "trip_detail/{tripId}",
                        arguments = listOf(navArgument("tripId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val tripId = backStackEntry.arguments?.getLong("tripId") ?: 0L
                        TripDetailScreen(
                            navController = navController,
                            tripId = tripId,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // 驾驶统计页面
                    composable("statistics") {
                        StatisticsScreen(
                            navController = navController,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // ✅ 系统设置页面
                    composable("setting") {
                        SettingScreen(
                            navController = navController,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // ✅ 附近服务区页面
                    composable("service_area") {
                        ServiceAreaScreen(
                            navController = navController,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }

        // 请求通知权限（Android 13+）
        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予
            }
        }
    }
}