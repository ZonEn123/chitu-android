package com.example.chitu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chitu.data.local.TokenManager
import com.example.chitu.viewmodel.SecurityUiState
import com.example.chitu.viewmodel.SecurityViewModel


private val ChituRed = Color(0xFFC62828)



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingScreen(
    navController: NavController
) {

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    val viewModel: SecurityViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>
            ): T {
                return SecurityViewModel(tokenManager) as T
            }
        }
    )


    val state by viewModel.uiState.collectAsState()


    var answer by remember {
        mutableStateOf("")
    }

    var newPwd by remember {
        mutableStateOf("")
    }

    var confirmPwd by remember {
        mutableStateOf("")
    }


    // 加载密保问题
    LaunchedEffect(Unit) {
        viewModel.getMySecurityQuestion()
    }


    // 修改成功重新登录
    LaunchedEffect(state) {

        if (state is SecurityUiState.Success) {

            navController.navigate("login") {
                popUpTo("login") {
                    inclusive = true
                }
            }
        }
    }


    val question =
        (state as? SecurityUiState.Question)?.question ?: ""


    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        "修改密码",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },


                navigationIcon = {

                    IconButton(
                        onClick = {

                            navController.popBackStack()
                            viewModel.reset()

                        }
                    ) {

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
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

    ) { inner ->


        Column(

            modifier = Modifier

                .fillMaxSize()

                // 先处理键盘避让
                .padding(inner)

                .imePadding()

                // 再滚动
                .verticalScroll(
                    rememberScrollState()
                )

                .padding(24.dp),


            verticalArrangement = Arrangement.Top

        ) {


            when(state){


                is SecurityUiState.Loading -> {


                    Box(

                        modifier = Modifier.fillMaxSize(),

                        contentAlignment = Alignment.Center

                    ){

                        CircularProgressIndicator(
                            color = ChituRed
                        )

                    }

                }



                is SecurityUiState.Error -> {


                    Text(

                        text =
                            (state as SecurityUiState.Error).message,

                        color = ChituRed

                    )

                }



                is SecurityUiState.Question -> {


                    // =========================
                    // 密保问题
                    // =========================

                    Text(

                        text = "密保问题",

                        fontSize = 13.sp,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )


                    Spacer(
                        Modifier.height(6.dp)
                    )


                    Surface(

                        shape = RoundedCornerShape(12.dp),

                        color = MaterialTheme.colorScheme.surfaceVariant,

                        modifier = Modifier.fillMaxWidth()

                    ){


                        Text(

                            text = question,

                            modifier = Modifier.padding(16.dp),

                            fontSize = 15.sp,

                            color = MaterialTheme.colorScheme.onSurfaceVariant

                        )

                    }



                    Spacer(
                        Modifier.height(16.dp)
                    )



                    // =========================
                    // 密保答案
                    // =========================

                    OutlinedTextField(

                        value = answer,

                        onValueChange = {
                            answer = it
                        },


                        label = {
                            Text("密保答案")
                        },


                        singleLine = true,


                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),


                        shape = RoundedCornerShape(12.dp),


                        colors = OutlinedTextFieldDefaults.colors(

                            focusedBorderColor = ChituRed,

                            unfocusedBorderColor = MaterialTheme.colorScheme.outline

                        )

                    )



                    Spacer(
                        Modifier.height(12.dp)
                    )



                    // =========================
                    // 新密码
                    // =========================

                    OutlinedTextField(

                        value = newPwd,

                        onValueChange = {
                            newPwd = it
                        },


                        label = {
                            Text("新密码")
                        },


                        visualTransformation =
                            PasswordVisualTransformation(),


                        singleLine = true,


                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),


                        shape = RoundedCornerShape(12.dp),


                        colors = OutlinedTextFieldDefaults.colors(

                            focusedBorderColor = ChituRed,

                            unfocusedBorderColor = MaterialTheme.colorScheme.outline

                        )

                    )



                    Spacer(
                        Modifier.height(12.dp)
                    )



                    // =========================
                    // 确认密码
                    // =========================

                    OutlinedTextField(

                        value = confirmPwd,

                        onValueChange = {
                            confirmPwd = it
                        },


                        label = {
                            Text("确认密码")
                        },


                        visualTransformation =
                            PasswordVisualTransformation(),


                        singleLine = true,


                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),


                        shape = RoundedCornerShape(12.dp),


                        colors = OutlinedTextFieldDefaults.colors(

                            focusedBorderColor = ChituRed,

                            unfocusedBorderColor = MaterialTheme.colorScheme.outline

                        )

                    )



                    Spacer(
                        Modifier.height(20.dp)
                    )



                    Button(

                        onClick = {

                            viewModel.changePassword(
                                answer,
                                newPwd,
                                confirmPwd
                            )

                        },


                        enabled =
                            state !is SecurityUiState.Loading,


                        modifier = Modifier

                            .fillMaxWidth()

                            .height(48.dp),


                        shape = RoundedCornerShape(12.dp),


                        colors = ButtonDefaults.buttonColors(

                            containerColor = ChituRed

                        )

                    ){

                        Text(

                            "确认修改",

                            color = Color.White,

                            fontSize = 16.sp

                        )

                    }

                }


                else -> {}

            }



            if(state is SecurityUiState.Error){

                Spacer(
                    Modifier.height(12.dp)
                )


                Text(

                    text =
                        (state as SecurityUiState.Error).message,


                    color = ChituRed,

                    fontSize = 14.sp

                )

            }

        }

    }

}