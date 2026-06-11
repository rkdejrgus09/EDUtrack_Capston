package com.example.capstone_3.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone_3.common.LocalThemeColor
import com.example.capstone_3.common.SubTopBar

// ── 비밀번호 변경 화면 ────────────────────────────────────────────────
@Composable
fun SettingsPasswordScreen(onBack: () -> Unit) {
    var current   by remember { mutableStateOf("") }
    var newPw     by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        SubTopBar("비밀번호 변경", onBack = onBack)
        HorizontalDivider(color = Color(0xFFF0F0F0))
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("현재 비밀번호", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = current, onValueChange = { current = it },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray)
                    )
                    Spacer(Modifier.height(16.dp))

                    Text("새 비밀번호", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newPw, onValueChange = { newPw = it },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray)
                    )
                    Spacer(Modifier.height(16.dp))

                    Text("새 비밀번호 확인", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = confirmPw, onValueChange = { confirmPw = it },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray)
                    )
                    Spacer(Modifier.height(20.dp))

                    Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalThemeColor.current)) {
                        Text("비밀번호 변경", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
