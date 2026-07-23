package com.example.chitu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.chitu.data.local.database.TripLogDatabase
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

private val ChituRed = Color(0xFFC62828)
private val PageBackground = Color(0xFFFAFAFA)
private val TextPrimary = Color(0xFF212121)
private val TextSecondary = Color(0xFF757575)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    navController: NavController,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val context = LocalContext.current
    val db = remember { TripLogDatabase.getInstance(context) }
    val tripsFlow = remember { db.tripLogDao().getAllTrips() }

    var trips by remember { mutableStateOf<List<com.example.chitu.data.local.entity.TripLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }

    val filteredTrips = if (searchQuery.isBlank()) {
        trips
    } else {
        trips.filter { trip ->
            trip.startLocation.contains(searchQuery, ignoreCase = true) ||
                    trip.endLocation.contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) {
        tripsFlow.collectLatest { tripList ->
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
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "开始你的第一次驾驶吧！",
                            fontSize = 14.sp,
                            color = TextSecondary
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
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "试试其他关键词",
                            fontSize = 14.sp,
                            color = TextSecondary
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
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("搜索起点或终点") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChituRed,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = ChituRed,
                            unfocusedLabelColor = Color.Gray
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索",
                                tint = ChituRed
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                TextButton(onClick = { searchQuery = "" }) {
                                    Text("清除", color = ChituRed, fontSize = 14.sp)
                                }
                            }
                        }
                    )

                    Text(
                        text = "共 ${filteredTrips.size} 条行程",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    color = TextPrimary
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
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "→",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Text(
                    text = "🏁 ${trip.endLocation.takeIf { it.isNotBlank() && it != "未知位置" } ?: "未知终点"}",
                    fontSize = 14.sp,
                    color = TextSecondary,
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
                    color = TextSecondary
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