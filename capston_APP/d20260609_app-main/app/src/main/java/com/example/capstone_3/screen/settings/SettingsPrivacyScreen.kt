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

// ── 개인정보 처리방침 화면 ────────────────────────────────────────────
@Composable
fun SettingsPrivacyScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SubTopBar("개인정보 처리방침", onBack = onBack)
        HorizontalDivider(color = Color(0xFFF0F0F0))
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            val sections = listOf(
                "1. 수집하는 개인정보 항목" to "회사는 서비스 제공을 위해 다음과 같은 개인정보를 수집합니다:\n- 필수항목: 이름, 이메일, 학교정보(학년·학번·반)\n- 선택항목: 프로필 사진, 연락처",
                "2. 개인정보의 수집 및 이용 목적" to "- 회원 관리 및 본인 확인\n- 학생-교사 간 상담 서비스 제공\n- 학생 정보 조회\n- 공지사항 및 일정 안내\n- 서비스 개선 및 통계 분석",
                "3. 개인정보의 보유 및 이용 기간" to "회사는 원칙적으로 개인정보 수집 및 이용 목적이 달성된 후에는 해당 정보를 지체 없이 파기합니다. 단, 관련 법령에 따라 보존할 필요가 있는 경우는 예외로 합니다.",
                "4. 개인정보의 제3자 제공" to "회사는 원칙적으로 이용자의 개인정보를 제3자에게 제공하지 않습니다. 다만, 회원이 동의하거나 법령에 의한 경우에는 예외로 합니다.",
                "5. 개인정보 보호책임자" to "이름: 홍길동\n직책: 개인정보보호책임자\n이메일: privacy@example.com"
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
