package com.example.capstone_3.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.capstone_3.common.LocalThemeColor
import com.example.capstone_3.common.SubTopBar

// ── 고객센터 화면 ─────────────────────────────────────────────────────
@Composable
fun SettingsSupportScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SubTopBar("고객센터", onBack = onBack)
        HorizontalDivider(color = Color(0xFFF0F0F0))
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("문의하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))

                    listOf(
                        Triple(Icons.Default.Email, "이메일 문의", "support@example.com"),
                        Triple(Icons.Default.Phone, "전화 문의", "1588-0000 (평일 09:00-18:00)"),
                        Triple(Icons.Default.MailOutline, "채팅 상담", "실시간 채팅 상담 (평일 09:00-18:00)")
                    ).forEach { (icon, title, desc) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, tint = LocalThemeColor.current, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(desc,  fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("자주 묻는 질문", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    listOf(
                        "비밀번호를 잊어버렸어요" to "로그인 화면에서 '비밀번호 찾기'를 클릭하여 등록된 이메일로 비밀번호를 재설정할 수 있습니다.",
                        "학생 정보를 수정하고 싶어요" to "학교 관리자에게 문의하여 학생 정보를 수정할 수 있습니다.",
                        "알림이 오지 않아요" to "환경설정 > 알림 설정에서 알림이 켜져 있는지 확인해주세요. 또한 기기의 알림 설정도 확인해주세요."
                    ).forEachIndexed { idx, (question, answer) ->
                        Text(question, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(answer, fontSize = 13.sp, color = Color.Gray, lineHeight = 19.sp)
                        if (idx < 2) {
                            HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
            }
        }
    }
}
