package com.example.capstone_3.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone_3.common.LocalThemeColor
import com.example.capstone_3.common.SubTopBar

// ── 알림 설정 화면 ────────────────────────────────────────────────────
@Composable
fun SettingsNotificationScreen(onBack: () -> Unit) {
    var noticeOn  by remember { mutableStateOf(true) }
    var studentOn by remember { mutableStateOf(true) }
    var counselOn by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        SubTopBar("알림 설정", onBack = onBack)
        HorizontalDivider(color = Color(0xFFF0F0F0))
        Column(modifier = Modifier.padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column {
                    NotificationToggleRow("공지사항 알림", "학교 공지사항 알림을 받습니다", noticeOn)  { noticeOn  = it }
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                    NotificationToggleRow("학생정보 알림", "학생정보 업데이트 알림을 받습니다", studentOn) { studentOn = it }
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                    NotificationToggleRow("상담 알림",    "상담 일정 알림을 받습니다", counselOn) { counselOn = it }
                }
            }
        }
    }
}

@Composable
fun NotificationToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = LocalThemeColor.current)
        )
    }
}
