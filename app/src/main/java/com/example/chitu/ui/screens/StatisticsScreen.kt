package com.example.chitu.ui.screens

import android.graphics.Color
import androidx.compose.foundation.background
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
import com.example.chitu.viewmodel.DrivingStatistics
import com.example.chitu.viewmodel.StatisticsUiState
import com.example.chitu.viewmodel.StatisticsViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import androidx.compose.ui.viewinterop.AndroidView

private val ChituRed = ComposeColor(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navController: NavController,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val context = LocalContext.current

    val viewModel: StatisticsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return StatisticsViewModel(context) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "驾驶统计",
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
        when (uiState) {
            is StatisticsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ChituRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "加载统计数据...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is StatisticsUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无驾驶数据",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "完成一次驾驶后，统计数据将自动生成",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = ChituRed)
                        ) {
                            Text("刷新", color = ComposeColor.White)
                        }
                    }
                }
            }

            is StatisticsUiState.Success -> {
                val data = (uiState as StatisticsUiState.Success).data
                StatisticsContent(
                    data = data,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is StatisticsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (uiState as StatisticsUiState.Error).message,
                            fontSize = 16.sp,
                            color = ComposeColor.Red
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = ChituRed)
                        ) {
                            Text("重试", color = ComposeColor.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsContent(
    data: DrivingStatistics,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val durationMinutes = data.totalDurationSeconds / 60
    val durationText = if (durationMinutes >= 60) {
        "${durationMinutes / 60}小时${durationMinutes % 60}分钟"
    } else {
        "${durationMinutes}分钟"
    }
    val distanceText = if (data.totalDistanceMeters >= 1000) {
        String.format("%.2f 公里", data.totalDistanceMeters / 1000)
    } else {
        "${data.totalDistanceMeters.toInt()} 米"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ✅ 顶部间距，解决卡片被遮挡问题
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 四项核心指标（2x2 网格）
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "驾驶时长",
                    value = durationText,
                    icon = "⏱️",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "行驶里程",
                    value = distanceText,
                    icon = "📏",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "行程次数",
                    value = "${data.totalTrips} 次",
                    icon = "📋",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "疲劳提醒",
                    value = "${data.totalFatigue} 次",
                    icon = "⚠️",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 近7天趋势图
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "📈 近7天驾驶趋势",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (data.weeklyData.all { it.durationSeconds == 0 }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "近7天暂无驾驶记录",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // 柱状图
                        AndroidView(
                            factory = { context ->
                                BarChart(context).apply {
                                    setTouchEnabled(false)
                                    description.isEnabled = false
                                    setPinchZoom(false)
                                    setScaleEnabled(false)
                                    setDrawGridBackground(false)
                                    setDrawBarShadow(false)

                                    // X 轴
                                    xAxis.apply {
                                        position = XAxis.XAxisPosition.BOTTOM
                                        setDrawGridLines(false)
                                        textSize = 11f
                                        textColor = Color.GRAY
                                        granularity = 1f
                                        setLabelCount(7, true)
                                        valueFormatter = IndexAxisValueFormatter(
                                            data.weeklyData.map { it.date }
                                        )
                                    }

                                    // Y 轴（左侧）
                                    axisLeft.apply {
                                        setDrawGridLines(true)
                                        textSize = 11f
                                        textColor = Color.GRAY
                                        setDrawZeroLine(false)
                                        axisMinimum = 0f
                                    }

                                    axisRight.isEnabled = false

                                    // 图例
                                    legend.isEnabled = false

                                    // 额外间距
                                    setExtraOffsets(16f, 16f, 16f, 16f)
                                }
                            },
                            update = { barChart ->
                                val entries = data.weeklyData.mapIndexed { index, day ->
                                    BarEntry(index.toFloat(), day.durationSeconds / 60f) // 转换为分钟
                                }

                                val dataSet = BarDataSet(entries, "驾驶时长（分钟）").apply {
                                    color = Color.parseColor("#C62828")
                                    setDrawValues(true)
                                    valueTextSize = 11f
                                    valueTextColor = Color.GRAY
                                }

                                val barData = BarData(dataSet)
                                barData.barWidth = 0.5f

                                barChart.data = barData
                                barChart.invalidate()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    }
                }
            }
        }

        // 刷新按钮
        item {
            Button(
                onClick = onRefresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ChituRed)
            ) {
                Text(
                    text = "刷新数据",
                    fontSize = 16.sp,
                    color = ComposeColor.White
                )
            }
        }

        // 底部留白
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = icon,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}