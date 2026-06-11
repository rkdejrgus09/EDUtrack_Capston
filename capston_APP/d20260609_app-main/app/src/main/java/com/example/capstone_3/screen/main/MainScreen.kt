package com.example.capstone_3.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone_3.auth.UserData
import com.example.capstone_3.common.BottomTab
import com.example.capstone_3.common.LightGray
import com.example.capstone_3.common.LocalThemeColor
import com.example.capstone_3.screen.settings.*


// 하위 페이지의 뒤로가기 대상
private fun settingsBack(current: String?): String? = when (current) {
    "Theme", "Account", "Notification", "AppInfo" -> "Settings"
    "Password"                                     -> "Account"
    "Terms", "Privacy", "Support"                  -> "AppInfo"
    else                                           -> null
}

// 화면 전환 방향 판단용 순위 (탭=0, 설정으로 들어갈수록 큰 값 → 오른쪽에서 슬라이드 등장)
private fun navRank(settingsNav: String?): Int = when (settingsNav) {
    null                                      -> 0
    "Settings"                                -> 10
    "Password", "Terms", "Privacy", "Support" -> 12
    else                                      -> 11
}

@Composable
fun MainScreen(
    userData: UserData?,
    onLogout: () -> Unit,
    onThemeChange: (androidx.compose.ui.graphics.Color) -> Unit = {}
) {
    var currentTab       by remember { mutableStateOf(BottomTab.Home) }
    var settingsNav      by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val onSettings: () -> Unit = { settingsNav = "Settings" }
    val onLogoutAction: () -> Unit = { showLogoutDialog = true }

    val swipeThreshold = with(LocalDensity.current) { 60.dp.toPx() }

    val tabs = BottomTab.values()
    val pagerState = rememberPagerState(initialPage = currentTab.ordinal) { tabs.size }

    // 바텀바 클릭으로 탭이 바뀌면 페이저도 부드럽게 이동
    LaunchedEffect(currentTab) {
        if (pagerState.currentPage != currentTab.ordinal) {
            pagerState.animateScrollToPage(currentTab.ordinal)
        }
    }
    // 손가락 스와이프가 멈추면(정착) 바텀바 하이라이트 동기화
    LaunchedEffect(pagerState.settledPage) {
        val settled = tabs[pagerState.settledPage]
        if (settled != currentTab) currentTab = settled
    }

    BackHandler(enabled = settingsNav != null) {
        settingsNav = settingsBack(settingsNav)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Color(0xFF1A1A1A),
                    selectedTextColor   = Color(0xFF1A1A1A),
                    indicatorColor      = LocalThemeColor.current.copy(alpha = 0.20f),
                    unselectedIconColor = Color(0xFFAAAAAA),
                    unselectedTextColor = Color(0xFFAAAAAA)
                )
                NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                    NavigationBarItem(
                        selected = settingsNav == null && currentTab == BottomTab.Home,
                        onClick  = { currentTab = BottomTab.Home; settingsNav = null },
                        icon     = { Icon(Icons.Default.Home, contentDescription = "홈") },
                        label    = { Text("홈", fontSize = 11.sp, fontWeight = if (settingsNav == null && currentTab == BottomTab.Home) FontWeight.Bold else FontWeight.Normal) },
                        colors   = navItemColors
                    )
                    NavigationBarItem(
                        selected = settingsNav == null && currentTab == BottomTab.Exam,
                        onClick  = { currentTab = BottomTab.Exam; settingsNav = null },
                        icon     = { Icon(Icons.Default.Edit, contentDescription = "시험") },
                        label    = { Text("시험", fontSize = 11.sp, fontWeight = if (settingsNav == null && currentTab == BottomTab.Exam) FontWeight.Bold else FontWeight.Normal) },
                        colors   = navItemColors
                    )
                    NavigationBarItem(
                        selected = settingsNav == null && currentTab == BottomTab.Grade,
                        onClick  = { currentTab = BottomTab.Grade; settingsNav = null },
                        icon     = { Icon(Icons.Default.Star, contentDescription = "성적") },
                        label    = { Text("성적", fontSize = 11.sp, fontWeight = if (settingsNav == null && currentTab == BottomTab.Grade) FontWeight.Bold else FontWeight.Normal) },
                        colors   = navItemColors
                    )
                    NavigationBarItem(
                        selected = settingsNav == null && currentTab == BottomTab.StudentInfo,
                        onClick  = { currentTab = BottomTab.StudentInfo; settingsNav = null },
                        icon     = { Icon(Icons.Default.Person, contentDescription = "학생 정보") },
                        label    = { Text("학생 정보", fontSize = 11.sp, fontWeight = if (settingsNav == null && currentTab == BottomTab.StudentInfo) FontWeight.Bold else FontWeight.Normal) },
                        colors   = navItemColors
                    )
                }
            },
            containerColor = LightGray
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                AnimatedContent(
                    targetState = settingsNav,
                    transitionSpec = {
                        val forward = navRank(targetState) >= navRank(initialState)
                        if (forward) {
                            (slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(200)))
                        } else {
                            (slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally(tween(300)) { it } + fadeOut(tween(200)))
                        }
                    },
                    label = "main_nav"
                ) { nav ->
                    if (nav == null) {
                        // ── 4개 탭: 손가락을 따라 실시간으로 움직이는 페이저 ──
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            when (tabs[page]) {
                                BottomTab.Home        -> HomeTab(userData = userData, onSettings = onSettings, onLogout = onLogoutAction)
                                BottomTab.Exam        -> ExamTab(userData = userData, onSettings = onSettings, onLogout = onLogoutAction)
                                BottomTab.Grade       -> GradeTab(userData = userData, onSettings = onSettings, onLogout = onLogoutAction)
                                BottomTab.StudentInfo -> StudentInfoTab(userData = userData, onSettings = onSettings, onLogout = onLogoutAction)
                            }
                        }
                    } else {
                        // ── 설정 등 하위 페이지: 오른쪽으로 스와이프하면 뒤로가기 ──
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(nav) {
                                    var dragTotal = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { dragTotal = 0f },
                                        onHorizontalDrag = { _, delta -> dragTotal += delta },
                                        onDragEnd = {
                                            if (dragTotal >= swipeThreshold) settingsNav = settingsBack(settingsNav)
                                        }
                                    )
                                }
                        ) {
                            when (nav) {
                                "Settings"     -> SettingsScreen(
                                    onBack         = { settingsNav = null },
                                    onAccount      = { settingsNav = "Account" },
                                    onNotification = { settingsNav = "Notification" },
                                    onTheme        = { settingsNav = "Theme" },
                                    onAppInfo      = { settingsNav = "AppInfo" },
                                    onLogout       = { showLogoutDialog = true }
                                )
                                "Theme"        -> SettingsThemeScreen(
                                    onBack        = { settingsNav = "Settings" },
                                    onThemeChange = onThemeChange
                                )
                                "Account"      -> SettingsAccountScreen(
                                    userData   = userData,
                                    onBack     = { settingsNav = "Settings" },
                                    onPassword = { settingsNav = "Password" }
                                )
                                "Password"     -> SettingsPasswordScreen(onBack = { settingsNav = "Account" })
                                "Notification" -> SettingsNotificationScreen(onBack = { settingsNav = "Settings" })
                                "AppInfo"      -> SettingsAppInfoScreen(
                                    onBack    = { settingsNav = "Settings" },
                                    onTerms   = { settingsNav = "Terms" },
                                    onPrivacy = { settingsNav = "Privacy" },
                                    onSupport = { settingsNav = "Support" }
                                )
                                "Terms"   -> SettingsTermsScreen(onBack = { settingsNav = "AppInfo" })
                                "Privacy" -> SettingsPrivacyScreen(onBack = { settingsNav = "AppInfo" })
                                "Support" -> SettingsSupportScreen(onBack = { settingsNav = "AppInfo" })
                            }
                        }
                    }
                }
            }
        }

        if (showLogoutDialog) {
            LogoutDialog(
                onConfirm = { showLogoutDialog = false; onLogout() },
                onDismiss = { showLogoutDialog = false }
            )
        }
    }
}


@Composable
fun LogoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .background(Color(0xFF2C2C2C), RoundedCornerShape(20.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("로그아웃 하시겠습니까?",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalThemeColor.current)
                    ) { Text("예", color = Color.White, fontWeight = FontWeight.Medium) }
                    Button(onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A4A4A))
                    ) { Text("아니오", color = Color.White, fontWeight = FontWeight.Medium) }
                }
            }
        }
    }
}

