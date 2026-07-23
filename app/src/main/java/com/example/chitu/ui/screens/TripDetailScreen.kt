package com.example.chitu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.chitu.data.local.database.TripLogDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private val ChituRed = Color(0xFFC62828)
private val PageBackground = Color(0xFFFAFAFA)
private val TextPrimary = Color(0xFF212121)
private val TextSecondary = Color(0xFF757575)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    navController: NavController,
    tripId: Long,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val context = LocalContext.current
    val db = remember { TripLogDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())

    var trip by remember { mutableStateOf<com.example.chitu.data.local.entity.TripLog?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showEditRemarkDialog by remember { mutableStateOf(false) }
    var tempRemark by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }

    fun loadTrip() {
        scope.launch {
            isLoading = true
            try {
                val result = withContext(Dispatchers.IO) {
                    db.tripLogDao().getTripById(tripId)
                }
                trip = result
                if (result == null) {
                    errorMessage = "行程不存在"
                }
            } catch (e: Exception) {
                errorMessage = "加载失败：${e.message}"
            }
            isLoading = false
        }
    }

    LaunchedEffect(tripId) {
        loadTrip()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "行程详情",
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
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除行程",
                            tint = Color.Red
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
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ChituRed)
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(errorMessage!!, fontSize = 16.sp, color = Color.Red)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(containerColor = ChituRed)
                        ) {
                            Text("返回", color = Color.White)
                        }
                    }
                }
            }
            trip != null -> {
                val data = trip!!
                val durationMinutes = data.durationSeconds / 60
                val durationText = if (durationMinutes >= 60) {
                    "${durationMinutes / 60}小时${durationMinutes % 60}分钟"
                } else {
                    "${durationMinutes}分钟"
                }
                val distanceText = if (data.distanceMeters >= 1000) {
                    String.format("%.2f 公里", data.distanceMeters / 1000)
                } else {
                    "${data.distanceMeters.toInt()} 米"
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (data.fatigueFlag == 1) {
                                    Text(
                                        text = "⚠️ 已触发疲劳提醒",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFF9800)
                                    )
                                } else {
                                    Text(
                                        text = "✅ 正常驾驶",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                                Text(
                                    text = "已完成",
                                    fontSize = 14.sp,
                                    color = Color(0xFF4CAF50)
                                )
                            }

                            Divider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                color = Color.LightGray
                            )

                            InfoRow("开始时间", dateFormat.format(Date(data.startTime)))
                            InfoRow("结束时间", dateFormat.format(Date(data.endTime)))
                            InfoRow("驾驶时长", durationText)

                            Divider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Color.LightGray
                            )

                            InfoRow(
                                "起点",
                                data.startLocation.takeIf { it.isNotBlank() && it != "未知位置" } ?: "未知起点"
                            )
                            InfoRow(
                                "终点",
                                data.endLocation.takeIf { it.isNotBlank() && it != "未知位置" } ?: "未知终点"
                            )

                            Divider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Color.LightGray
                            )

                            InfoRow("行驶里程", distanceText)

                            Divider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Color.LightGray
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "备注",
                                    fontSize = 15.sp,
                                    color = TextSecondary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = data.remark.takeIf { it.isNotBlank() } ?: "点击添加备注",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (data.remark.isNotBlank()) TextPrimary else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            tempRemark = data.remark
                                            showEditRemarkDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "编辑备注",
                                            tint = ChituRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChituRed)
                    ) {
                        Text("返回", fontSize = 16.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // ✅ 备注编辑对话框（与个人信息弹窗风格一致）
    if (showEditRemarkDialog) {
        AlertDialog(
            onDismissRequest = { showEditRemarkDialog = false },
            title = {
                Text(
                    text = "编辑备注",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = tempRemark,
                    onValueChange = { tempRemark = it },
                    label = { Text("请输入备注") },
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChituRed,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    db.tripLogDao().updateRemark(tripId, tempRemark)
                                }
                                loadTrip()
                                showEditRemarkDialog = false
                            } catch (e: Exception) {
                                // 可以显示错误提示
                            }
                        }
                    }
                ) {
                    Text("保存", color = ChituRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditRemarkDialog = false }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }

    // ✅ 删除确认对话框（与个人信息弹窗风格一致）
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "删除行程",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "确定要删除这条行程记录吗？此操作无法撤销。",
                    fontSize = 15.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    db.tripLogDao().deleteById(tripId)
                                }
                                navController.popBackStack()
                            } catch (e: Exception) {
                                // 可以显示错误提示
                            }
                        }
                    }
                ) {
                    Text("删除", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}