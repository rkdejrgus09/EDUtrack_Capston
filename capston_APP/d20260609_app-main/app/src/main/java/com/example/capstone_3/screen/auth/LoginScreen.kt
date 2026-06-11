package com.example.capstone_3.screen.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone_3.R
import com.example.capstone_3.auth.AuthState
import com.example.capstone_3.auth.AuthViewModel

private val BgDark  = Color(0xFF111111)
private val BtnDark = Color(0xFF1A1A1A)

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToSignUp: () -> Unit,
    onLoginSuccess: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    var showLoginForm by remember { mutableStateOf(false) }
    var id       by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            viewModel.resetState()
            onLoginSuccess()
        }
    }

    BackHandler(enabled = showLoginForm) {
        showLoginForm = false
        viewModel.resetState()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // 슬라이딩 텍스트 배경
        Row(modifier = Modifier.fillMaxSize()) {
            ScrollingTextCol(scrollUp = true,  align = Alignment.Start, modifier = Modifier.weight(1f).fillMaxHeight())
            ScrollingTextCol(scrollUp = false, align = Alignment.End,   modifier = Modifier.weight(1f).fillMaxHeight())
        }

        AnimatedContent(
            targetState = showLoginForm,
            transitionSpec = {
                if (targetState) {
                    (slideInVertically(tween(380)) { it / 3 } + fadeIn(tween(300))) togetherWith
                    (slideOutVertically(tween(280)) { -it / 6 } + fadeOut(tween(200)))
                } else {
                    (slideInVertically(tween(380)) { -it / 3 } + fadeIn(tween(300))) togetherWith
                    (slideOutVertically(tween(280)) { it / 6 } + fadeOut(tween(200)))
                }
            },
            label = "login_anim"
        ) { isForm ->
            if (isForm) {
                LoginFormContent(
                    viewModel        = viewModel,
                    authState        = authState,
                    id               = id,
                    onIdChange       = { id = it; viewModel.resetState() },
                    password         = password,
                    onPasswordChange = { password = it; viewModel.resetState() },
                    onLogin          = { viewModel.login(id, password) },
                    onForgotPassword = onForgotPassword,
                    onBack           = { showLoginForm = false; viewModel.resetState() }
                )
            } else {
                WelcomeContent(
                    onLoginClick  = { showLoginForm = true },
                    onSignUpClick = onNavigateToSignUp
                )
            }
        }
    }
}

// ── 웰컴 화면 ─────────────────────────────────────────────────────────
@Composable
private fun WelcomeContent(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter            = painterResource(R.mipmap.ic_launcher),
                    contentDescription = "앱 아이콘",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(22.dp))
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text          = "EDUTRACK",
                    color         = Color.White,
                    fontSize      = 26.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 4.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text      = "학생 성장을 함께합니다",
                    color     = Color.White.copy(alpha = 0.5f),
                    fontSize  = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick  = onLoginClick,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor   = BgDark
                )
            ) {
                Text("로그인", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                Text("  OR  ", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick  = onSignUpClick,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("회원가입", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── 로그인 폼 카드 ────────────────────────────────────────────────────
@Composable
private fun LoginFormContent(
    viewModel: AuthViewModel,
    authState: AuthState,
    id: String,             onIdChange: (String) -> Unit,
    password: String,       onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth(0.88f),
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column {
                // ── 좌측 상단 뒤로가기 ────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, top = 10.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector  = Icons.Default.ArrowBack,
                            contentDescription = "뒤로",
                            tint         = Color.DarkGray
                        )
                    }
                }

                // ── 중앙 콘텐츠 ───────────────────────────────────────
                Column(
                    modifier            = Modifier
                        .padding(start = 28.dp, end = 28.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 실제 앱 아이콘
                    Image(
                        painter            = painterResource(R.mipmap.ic_launcher),
                        contentDescription = "앱 아이콘",
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                    Spacer(Modifier.height(18.dp))

                    Text(
                        text       = "로그인",
                        fontSize   = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.Black
                    )
                    Spacer(Modifier.height(28.dp))

                    OutlinedTextField(
                        value         = id,
                        onValueChange = onIdChange,
                        label         = { Text("이메일") },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = BtnDark,
                            unfocusedBorderColor = Color(0xFFDDDDDD),
                            focusedLabelColor    = BtnDark
                        )
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value                = password,
                        onValueChange        = onPasswordChange,
                        label                = { Text("비밀번호") },
                        modifier             = Modifier.fillMaxWidth(),
                        shape                = RoundedCornerShape(12.dp),
                        singleLine           = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors               = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = BtnDark,
                            unfocusedBorderColor = Color(0xFFDDDDDD),
                            focusedLabelColor    = BtnDark
                        )
                    )

                    if (authState is AuthState.Error) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            (authState as AuthState.Error).message,
                            color = Color(0xFFE53935), fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick  = onLogin,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = BtnDark,
                            contentColor   = Color.White
                        ),
                        enabled = authState !is AuthState.Loading
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                color       = Color.White,
                                strokeWidth = 2.dp,
                                modifier    = Modifier.size(22.dp)
                            )
                        } else {
                            Text("로그인", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(18.dp))

                    // 비밀번호 찾기 (좌측 정렬)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "비밀번호 찾기",
                            fontSize = 13.sp,
                            color    = Color.Gray,
                            modifier = Modifier.clickable { onForgotPassword() }
                        )
                    }
                }
            }
        }
    }
}

// ── 무한 스크롤 텍스트 컬럼 ───────────────────────────────────────────
// 원리: 텍스트 한 줄(tile)이 반복 단위. translationY 를 정확히 tile 1개 높이만큼
//       이동시키고 RepeatMode.Restart 로 반복 → 끝과 시작 모습이 동일해 끊김 없음.
@Composable
private fun ScrollingTextCol(
    scrollUp: Boolean,
    align: Alignment.Horizontal,
    modifier: Modifier = Modifier
) {
    val density    = LocalDensity.current
    val tileHeight = 110.dp
    val tilePx     = with(density) { tileHeight.toPx() }

    val transition = rememberInfiniteTransition(label = if (scrollUp) "up" else "dn")
    // progress: 0f → 1f 를 무한 반복 (tile 1개 분량의 이동)
    val progress by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p"
    )
    // 위로: 0 → -tile / 아래로: 0 → +tile
    val shift = if (scrollUp) -progress * tilePx else progress * tilePx

    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        // 화면을 채우고 이동분(tile 1개)까지 여유 두려면 +3
        val count = (maxHeight / tileHeight).toInt() + 3

        Column(
            modifier = Modifier.graphicsLayer {
                // 기본을 tile 1개만큼 위로 올려두고 shift 적용 → 위/아래 모두 빈공간 없음
                translationY = shift - tilePx
            }
        ) {
            repeat(count) {
                Box(
                    modifier         = Modifier.height(tileHeight).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = "EDUTRACK",
                        fontSize   = 80.sp,
                        fontWeight = FontWeight.Black,
                        color      = Color.White.copy(alpha = 0.13f),
                        softWrap   = false,
                        maxLines   = 1,
                        // 본래 크기로 그리되 align 쪽 절반만 보이게 (왼:EDUT / 오:RACK)
                        modifier   = Modifier.wrapContentWidth(align, unbounded = true)
                    )
                }
            }
        }
    }
}
