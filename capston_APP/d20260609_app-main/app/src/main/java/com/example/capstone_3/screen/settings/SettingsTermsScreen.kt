package com.example.capstone_3.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone_3.common.SubTopBar

// ── 이용약관 화면 ─────────────────────────────────────────────────────
@Composable
fun SettingsTermsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SubTopBar("이용약관", onBack = onBack)
        HorizontalDivider(color = Color(0xFFF0F0F0))
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            val sections = listOf(
                "제1조 (목적)" to "본 약관은 에듀트랙 소통 플랫폼(이하 \"서비스\")의 이용과 관련하여 회사와 회원 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.",
                "제2조 (정의)" to "1. \"서비스\"는 에듀트랙이 본 약관에 동의하고 서비스를 이용하는 학생 및 교사를 말합니다.\n2. \"학생 정보\"는 회원이 등록한 학생의 학교 및 학급 관련 정보를 말합니다.",
                "제3조 (서비스의 제공)" to "회사는 학생-교사 간 상담 예약, 학생 정보 조회, 공지사항 전달 등의 서비스를 제공합니다.",
                "제4조 (개인정보 보호)" to "회사는 관련 법령에 따라 회원의 개인정보를 보호하기 위해 노력하며, 개인정보의 보호 및 사용에 대해서는 개인정보 처리방침을 적용합니다."
            )
            sections.forEach { (title, content) ->
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(content, fontSize = 13.sp, color = Color.DarkGray, lineHeight = 20.sp)
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
