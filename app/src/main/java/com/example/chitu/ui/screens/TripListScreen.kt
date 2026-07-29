package com.example.chitu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
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
import com.example.chitu.data.local.TokenManager
import com.example.chitu.data.local.database.TripLogDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

private val ChituRed = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    navController: NavController,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val context = LocalContext.current
    val db = remember { TripLogDatabase.getInstance(context) }

    var trips by remember { mutableStateOf<List<com.example.chitu.data.local.entity.TripLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableIntStateOf(-1) } // -1=全部 0=进行中 1=已完成 2=异常

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    val filteredTrips = trips.filter { trip ->
        val matchSearch = searchQuery.isBlank() ||
                trip.startLocation.contains(searchQuery, ignoreCase = true) ||
                trip.endLocation.contains(searchQuery, ignoreCase = true)
        val matchStatus = statusFilter == -1 || trip.tripStatus == statusFilter
        matchSearch && matchStatus
    }

    LaunchedEffect(Unit) {
        val userId = TokenManager(context).getUserId() ?: return@LaunchedEffect
        db.tripLogDao().getTripsByUserId(userId).collectLatest { tripList ->
            trips = tripList
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "行程日志",
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
            trips.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无行程记录",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "开始你的第一次驾驶吧！",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            filteredTrips.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "未找到匹配的行程",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "试试其他关键词",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { searchQuery = "" }) {
                            Text("清除搜索", color = ChituRed)
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                ) {
                    // 搜索框
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("搜索起点或终点") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChituRed,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = ChituRed,
                            unfocusedLabelColor = Color.Gray
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "搜索", tint = ChituRed)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                TextButton(onClick = { searchQuery = "" }) {
                                    Text("清除", color = ChituRed, fontSize = 14.sp)
                                }
                            }
                        }
                    )

                    // 状态筛选
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(-1 to "全部", 0 to "进行中", 1 to "已完成", 2 to "异常").forEach { (value, label) ->
                            FilterChip(
                                selected = statusFilter == value,
                                onClick = { statusFilter = value },
                                label = { Text(label, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ChituRed.copy(alpha = 0.15f),
                                    selectedLabelColor = ChituRed
                                )
                            )
                        }
                    }

                    // 结果计数
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "共 ${filteredTrips.size} 条行程",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        if (statusFilter != -1 || searchQuery.isNotEmpty()) {
                            TextButton(onClick = { statusFilter = -1; searchQuery = "" }) {
                                Text("重置筛选", color = ChituRed, fontSize = 13.sp)
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredTrips) { trip ->
                            TripListItem(
                                trip = trip,
                                onClick = {
                                    navController.navigate("trip_detail/${trip.id}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TripListItem(
    trip: com.example.chitu.data.local.entity.TripLog,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val startTimeStr = dateFormat.format(Date(trip.startTime))
    val durationMinutes = trip.durationSeconds / 60
    val durationText = if (durationMinutes >= 60) {
        "${durationMinutes / 60}h${durationMinutes % 60}m"
    } else {
        "${durationMinutes}m"
    }
    val distanceText = if (trip.distanceMeters >= 1000) {
        String.format("%.2f km", trip.distanceMeters / 1000)
    } else {
        "${trip.distanceMeters.toInt()} m"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = startTimeStr,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = durationText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChituRed
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📍 ${trip.startLocation.takeIf { it.isNotBlank() && it != "未知位置" } ?: "未知起点"}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "→",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Text(
                    text = "🏁 ${trip.endLocation.takeIf { it.isNotBlank() && it != "未知位置" } ?: "未知终点"}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📏 $distanceText",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (trip.fatigueFlag == 1) {
                    Text(
                        text = "⚠️ 疲劳提醒",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}