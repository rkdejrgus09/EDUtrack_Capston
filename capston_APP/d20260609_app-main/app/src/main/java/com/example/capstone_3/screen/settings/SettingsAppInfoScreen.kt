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
import com.example.capstone_3.common.SubTopBar

// ── 앱 정보 화면 ──────────────────────────────────────────────────────
@Composable
fun SettingsAppInfoScreen(onBack: () -> Unit, onTerms: () -> Unit, onPrivacy: () -> Unit, onSupport: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SubTopBar("앱 정보", onBack = onBack)
        HorizontalDivider(color = Color(0xFFF0F0F0))
        Column(modifier = Modifier.padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("버전", fontSize = 14.sp, color = Color.Gray)
                        Text("1.2.1", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(vertical = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("최근 업데이트", fontSize = 14.sp, color = Color.Gray)
                        Text("2026.06.06", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column {
                    AppInfoRow("이용약관",        onClick = onTerms)
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                    AppInfoRow("개인정보 처리방침", onClick = onPrivacy)
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                    AppInfoRow("고객센터",         onClick = onSupport)
                }
            }
        }
    }
}

@Composable
fun AppInfoRow(label: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp)
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
    }
}
