package com.example.capstone_3.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone_3.auth.UserData
import com.example.capstone_3.common.TopBar

private fun formatBirthDate(raw: String): String {
    if (raw.length != 8) return raw
    val year  = raw.substring(0, 4)
    val month = raw.substring(4, 6).toIntOrNull() ?: return raw
    val day   = raw.substring(6, 8).toIntOrNull() ?: return raw
    return "${year}년 ${month}월 ${day}일"
}

@Composable
private fun StudentInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.width(72.dp))
        Text(value, fontSize = 13.sp, color = Color.DarkGray)
    }
}

@Composable
fun StudentInfoTab(userData: UserData?, onSettings: () -> Unit = {}, onLogout: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar("학생 정보", onSettings = onSettings, onLogout = onLogout)
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${userData?.childName ?: ""} 학생", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(Modifier.height(12.dp))
                    StudentInfoRow("학교", userData?.childSchool ?: "")
                    StudentInfoRow("학년/반/번호", "${userData?.childGrade ?: 0}학년 ${userData?.childClass ?: 0}반 ${userData?.childNumber ?: 0}번")
                    StudentInfoRow("생년월일", userData?.birthDate?.let { formatBirthDate(it) } ?: "")
                }
            }
        }
    }
}
