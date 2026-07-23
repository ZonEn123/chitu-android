package com.example.chitu.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chitu.data.model.ServiceArea
import com.example.chitu.data.repository.LocationRepository
import com.example.chitu.viewmodel.ServiceAreaUiState
import com.example.chitu.viewmodel.ServiceAreaViewModel

private val ChituRed = Color(0xFFC62828)
private val PageBackground = Color(0xFFFAFAFA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceAreaScreen(
    navController: NavController,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val context = LocalContext.current
    val viewModel: ServiceAreaViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ServiceAreaViewModel(LocationRepository(context)) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadServiceAreas() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("附近服务区", fontWeight = FontWeight.Bold, color = Color(0xFF212121)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = ChituRed)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = ChituRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBackground)
            )
        },
        containerColor = PageBackground
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                is ServiceAreaUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ChituRed)
                        Spacer(Modifier.height(16.dp))
                        Text("正在搜索附近服务区...", fontSize = 14.sp, color = Color(0xFF757575))
                    }
                }
                is ServiceAreaUiState.Success -> {
                    val data = (uiState as ServiceAreaUiState.Success).data
                    LazyColumn(
                        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(data) { sa ->
                            ServiceAreaItem(sa, onClick = {
                                Toast.makeText(context, "${sa.name}\n${sa.address}", Toast.LENGTH_SHORT).show()
                            }, onNavigate = {
                                val uri = Uri.parse("geo:0,0?q=${sa.latitude},${sa.longitude}(${sa.name})")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            })
                        }
                    }
                }
                is ServiceAreaUiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🗺️", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("附近没有找到服务区", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                        Spacer(Modifier.height(8.dp))
                        Text("试试扩大搜索范围或换个位置", fontSize = 14.sp, color = Color(0xFF757575))
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }, colors = ButtonDefaults.buttonColors(containerColor = ChituRed)) {
                            Text("刷新", color = Color.White)
                        }
                    }
                }
                is ServiceAreaUiState.Error -> {
                    val msg = (uiState as ServiceAreaUiState.Error).message
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("❌", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(msg, fontSize = 16.sp, color = Color.Red, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.refresh() }, colors = ButtonDefaults.buttonColors(containerColor = ChituRed)) {
                                Text("重试", color = Color.White)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ServiceAreaItem(sa: ServiceArea, onClick: () -> Unit, onNavigate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(sa.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121))
                Spacer(Modifier.height(4.dp))
                Text(sa.address, fontSize = 14.sp, color = Color(0xFF757575), maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(sa.getFormattedDistance(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ChituRed)
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onNavigate, modifier = Modifier.height(32.dp), colors = ButtonDefaults.textButtonColors(contentColor = ChituRed)) {
                    Text("导航", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
