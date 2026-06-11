package com.example.capstone_3.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone_3.auth.UserData
import com.example.capstone_3.common.LocalThemeColor
import com.example.capstone_3.common.SubTopBar
import com.example.capstone_3.common.TopBar

// ── 공지사항 데이터 모델 ─────────────────────────────────────────────
data class Notice(val title: String, val date: String, val content: String)

// 임시 공지사항 목록 (최신순)
private val notices = listOf(
    Notice(
        "2026학년도 1학기 기말고사 시간표 안내", "2026.06.05",
        "1학기 기말고사가 7월 1일(수)부터 7월 7일(화)까지 진행됩니다.\n과목별 세부 시간표는 시험 탭에서 확인하실 수 있으며, 시험 당일 09:00까지 등교 바랍니다."
    ),
    Notice(
        "여름방학 방과후학교 수강 신청 안내", "2026.06.03",
        "여름방학 방과후학교 수강 신청을 6월 10일(수)부터 6월 17일(수)까지 받습니다.\n신청은 담임 선생님을 통해 진행되며, 강좌별 정원이 마감될 수 있으니 서둘러 신청해 주세요."
    ),
    Notice(
        "교내 체육대회 개최 안내", "2026.05.29",
        "제38회 교내 체육대회가 6월 20일(금) 운동장에서 개최됩니다.\n학급별 단체복 및 준비물 안내는 추후 공지될 예정이며, 우천 시 일정이 변경될 수 있습니다."
    ),
    Notice(
        "6월 모의고사 응시 안내", "2026.05.25",
        "전국연합학력평가(모의고사)가 6월 4일(목) 실시됩니다.\n응시 과목 및 준비물(컴퓨터용 사인펜, 신분증)을 미리 확인하시기 바랍니다."
    ),
    Notice(
        "학부모 상담 주간 운영 안내", "2026.05.20",
        "학부모 상담 주간을 6월 9일(화)부터 6월 13일(토)까지 운영합니다.\n상담 예약은 상담 메뉴에서 가능하며, 희망 시간대를 선택해 신청해 주세요."
    ),
    Notice(
        "급식 만족도 설문조사 실시", "2026.05.15",
        "5월 급식 만족도 설문조사를 실시합니다.\n학생 여러분의 소중한 의견은 더 나은 급식 운영에 반영됩니다. 5월 22일까지 참여 부탁드립니다."
    ),
    Notice(
        "도서관 야간 개방 운영 안내", "2026.05.12",
        "기말고사 대비 기간 동안 학교 도서관을 21:00까지 연장 운영합니다.\n운영 기간은 6월 15일부터 6월 30일까지이며, 이용 시 학생증을 지참해 주세요."
    ),
    Notice(
        "교복 착용 및 용의복장 점검 안내", "2026.05.08",
        "단정한 학교생활 문화 조성을 위해 용의복장 점검을 실시합니다.\n점검은 매주 월요일 아침 조회 시간에 진행되며, 동복·하복 혼용 기간을 준수해 주세요."
    ),
    Notice(
        "현장체험학습 신청서 제출 안내", "2026.05.04",
        "가정에서 실시하는 현장체험학습 신청서는 체험 5일 전까지 담임 선생님께 제출해야 합니다.\n사후 보고서도 기한 내 제출해 주시기 바랍니다."
    ),
    Notice(
        "학교 홈페이지 및 앱 점검 안내", "2026.05.01",
        "서비스 안정화를 위한 시스템 점검이 5월 3일(일) 02:00~06:00 진행됩니다.\n점검 시간 동안 앱 이용이 일시적으로 제한될 수 있는 점 양해 부탁드립니다."
    )
)

@Composable
fun HomeTab(userData: UserData?, onSettings: () -> Unit = {}, onLogout: () -> Unit = {}) {
    // 홈 내부 화면 전환 상태: 전체 목록 / 상세
    var showAllNotices by remember { mutableStateOf(false) }
    var selectedNotice by remember { mutableStateOf<Notice?>(null) }

    BackHandler(enabled = showAllNotices || selectedNotice != null) {
        when {
            selectedNotice != null -> selectedNotice = null
            else                   -> showAllNotices = false
        }
    }

    when {
        selectedNotice != null -> NoticeDetailScreen(
            notice = selectedNotice!!,
            onBack = { selectedNotice = null }
        )
        showAllNotices -> NoticeListScreen(
            onBack   = { showAllNotices = false },
            onSelect = { selectedNotice = it }
        )
        else -> HomeContent(
            userData       = userData,
            onSettings     = onSettings,
            onLogout       = onLogout,
            onOpenNotices  = { showAllNotices = true }
        )
    }
}

// ── 홈 메인 ───────────────────────────────────────────────────────────
@Composable
private fun HomeContent(
    userData: UserData?,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    onOpenNotices: () -> Unit
) {
    val themeColor = LocalThemeColor.current
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("메인 화면", onSettings = onSettings, onLogout = onLogout)
        Column(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                val greeting = if (userData?.isAdmin == true)
                    "관리자님, 반갑습니다."
                else
                    "${userData?.childName ?: ""} 학생, 반갑습니다."
                Text(greeting, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (userData?.isAdmin == true) "에듀트랙 관리자 계정입니다." else "나의 학습 현황을 확인하세요.",
                    fontSize = 13.sp, color = Color.Gray
                )
            }
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpenNotices() },
                shape  = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📋", fontSize = 15.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("최근 공지사항", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("전체보기", fontSize = 12.sp, color = themeColor, fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = themeColor, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    val recent = notices.take(4)
                    recent.forEachIndexed { idx, notice ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(notice.title, fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Text(notice.date, fontSize = 12.sp, color = Color.Gray)
                        }
                        if (idx < recent.lastIndex)
                            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

// ── 전체 공지사항 목록 ────────────────────────────────────────────────
@Composable
private fun NoticeListScreen(onBack: () -> Unit, onSelect: (Notice) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SubTopBar("공지사항", onBack = onBack)
        HorizontalDivider(color = Color(0xFFF0F0F0))
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            notices.forEach { notice ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(notice) }
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(notice.title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            color = Color(0xFF1A1A1A), modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(notice.date, fontSize = 12.sp, color = Color.Gray)
                }
                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
            }
        }
    }
}

// ── 공지사항 상세 ─────────────────────────────────────────────────────
@Composable
private fun NoticeDetailScreen(notice: Notice, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SubTopBar("공지사항", onBack = onBack)
        HorizontalDivider(color = Color(0xFFF0F0F0))
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            Text(notice.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
            Spacer(Modifier.height(8.dp))
            Text(notice.date, fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(Modifier.height(16.dp))
            Text(notice.content, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 22.sp)
        }
    }
}
