package com.example.capstone_3.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone_3.auth.UserData
import com.example.capstone_3.common.SubTopBar

// ── 계정 정보 화면 ────────────────────────────────────────────────────
@Composable
fun SettingsAccountScreen(userData: UserData?, onBack: () -> Unit, onPassword: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SubTopBar("계정 정보", onBack = onBack)
        HorizontalDivider(color = Color(0xFFF0F0F0))
        Column(modifier = Modifier.padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    listOf(
                        "학생 이름" to (userData?.childName ?: ""),
                        "학년 / 반" to "${userData?.childGrade ?: 0}학년 ${userData?.childClass ?: 0}반",
                        "학교"     to (userData?.childSchool ?: ""),
                        "이메일"   to (userData?.email ?: "")
                    ).forEachIndexed { idx, (label, value) ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(label, fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                        if (idx < 3) HorizontalDivider(color = Color(0xFFF5F5F5))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(modifier = Modifier.fillMaxWidth().clickable { onPassword() }
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("비밀번호 변경", fontSize = 15.sp)
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
                }
            }
        }
    }
}
