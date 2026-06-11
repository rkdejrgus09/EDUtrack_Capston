package com.example.capstone_3.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── 색상 상수 ────────────────────────────────────────────────────────
val Blue      = Color(0xFF4A90D9)
val LightGray = Color(0xFFF2F2F2)
val DarkNavy  = Color(0xFF1A1A2E)
val GreenTag  = Color(0xFF4CAF50)
val YellowTag = Color(0xFFFFC107)

// ── 테마 색상 (CompositionLocal) ──────────────────────────────────────
val LocalThemeColor = compositionLocalOf { Blue }

// ── 앱 전체 화면 열거형 ──────────────────────────────────────────────
enum class Screen {
    Login, ForgotPassword,
    SignUp,
    ChildInfoSchool,
    Main
}

enum class BottomTab { Home, Exam, Grade, StudentInfo }

// ── 공통 TopBar ───────────────────────────────────────────────────────
@Composable
fun TopBar(title: String, onSettings: () -> Unit = {}, onLogout: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "환경설정", tint = Color.DarkGray)
        }
        IconButton(onClick = onLogout) {
            Icon(Icons.Default.ExitToApp, contentDescription = "로그아웃", tint = Color(0xFFE53935))
        }
    }
}

// ── 설정 서브 화면 TopBar ─────────────────────────────────────────────
@Composable
fun SubTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.ArrowBack, contentDescription = "뒤로", tint = Color.DarkGray,
            modifier = Modifier.clickable { onBack() }
        )
        Spacer(Modifier.width(16.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}
