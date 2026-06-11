package com.example.capstone_3.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone_3.common.SubTopBar

// ── 환경설정 메인 화면 ────────────────────────────────────────────────
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAccount: () -> Unit,
    onNotification: () -> Unit,
    onTheme: () -> Unit,
    onAppInfo: () -> Unit,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SubTopBar("환경설정", onBack = onBack)
        Column(modifier = Modifier.padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column {
                    SettingsRow(icon = Icons.Default.Person, label = "계정 정보", onClick = onAccount)
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(icon = Icons.Default.Notifications, label = "알림 설정", onClick = onNotification)
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(icon = Icons.Default.Palette, label = "테마 설정", onClick = onTheme)
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(icon = Icons.Default.Info, label = "앱 정보", onClick = onAppInfo)
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Box(modifier = Modifier.fillMaxWidth().clickable { onLogout() }.padding(16.dp),
                    contentAlignment = Alignment.Center) {
                    Text("로그아웃", fontSize = 15.sp, color = Color(0xFFE53935), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
    }
}
