package com.example.chitu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chitu.ui.screens.HomeScreen
import com.example.chitu.ui.screens.LoginScreen
import com.example.chitu.ui.screens.ProfileScreen
import com.example.chitu.ui.screens.RegisterScreen
import com.example.chitu.ui.screens.SplashScreen
import com.example.chitu.ui.screens.TripDetailScreen
import com.example.chitu.ui.screens.TripListScreen
import com.example.chitu.ui.theme.ChituTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ChituTheme {
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

                    // 在 NavHost 中添加
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
                }
            }
        }

        // ✅ 请求通知权限（Android 13+）
        requestNotificationPermission()
        // 在 MainActivity 的 onCreate 中添加
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1001
            )
        }
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
                // 权限已授予，通知会正常显示
            }
        }
    }
}