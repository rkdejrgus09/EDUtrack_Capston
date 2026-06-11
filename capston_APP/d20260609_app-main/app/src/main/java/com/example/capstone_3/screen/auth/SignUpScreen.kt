package com.example.capstone_3.screen.auth

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone_3.common.LocalThemeColor

private val FieldBg = Color(0xFFF5F5F5)
private val DividerColor = Color(0xFFF0F0F0)

// ── ChildInfoScreens에서 공유하는 단계별 레이아웃 ────────────────────
@Composable
internal fun SignUpStepLayout(
    step: Int,
    totalSteps: Int,
    title: String,
    subtitle: String = "",
    isNextEnabled: Boolean,
    nextLabel: String = "다음",
    onNext: () -> Unit,
    onBack: () -> Unit,
    extraContent: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val themeColor = LocalThemeColor.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 28.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "뒤로", tint = Color.DarkGray)
            }
            Spacer(Modifier.weight(1f))
            Text("$step / $totalSteps", fontSize = 13.sp, color = Color.Gray)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { step.toFloat() / totalSteps },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = themeColor,
            trackColor = Color(0xFFE0E0E0)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.Black, lineHeight = 34.sp)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(subtitle, fontSize = 14.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(32.dp))
            content()
            extraContent()
        }
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isNextEnabled) themeColor else Color(0xFFCCCCCC),
                contentColor = Color.White
            ),
            enabled = isNextEnabled
        ) { Text(nextLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(Modifier.height(32.dp))
    }
}

// ── PASS 스타일 회원가입 플로우 (이메일→비밀번호→확인→이름→생년월일) ────────
@Composable
fun SignUpFlowScreen(
    step: Int,
    onStepChange: (Int) -> Unit,
    email: String,           onEmailChange: (String) -> Unit,
    password: String,        onPasswordChange: (String) -> Unit,
    passwordConfirm: String, onPasswordConfirmChange: (String) -> Unit,
    name: String,            onNameChange: (String) -> Unit,
    birthDate: String,       onBirthDateChange: (String) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val themeColor = LocalThemeColor.current

    var emailError by remember { mutableStateOf<String?>(null) }

    BackHandler { if (step > 0) onStepChange(step - 1) else onBack() }

    // 정식 이메일 형식 검증 (다음 버튼을 누를 때 확인)
    val emailValid = email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    fun advanceFromEmail() {
        if (emailValid) {
            emailError = null
            onStepChange(1)
        } else {
            emailError = "올바른 이메일 형식이 아니에요."
        }
    }

    val isCurrentValid = when (step) {
        0 -> email.isNotBlank()
        1 -> password.length >= 6
        2 -> passwordConfirm.isNotBlank() && passwordConfirm == password
        3 -> name.isNotBlank()
        4 -> birthDate.length == 8
        else -> false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 28.dp)
    ) {
        // ── 상단 바 / 진행률 ─────────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (step > 0) onStepChange(step - 1) else onBack() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "뒤로", tint = Color.DarkGray)
            }
            Spacer(Modifier.weight(1f))
            Text("${step + 1} / 6", fontSize = 13.sp, color = Color.Gray)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (step + 1).toFloat() / 6f },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = themeColor,
            trackColor = Color(0xFFE0E0E0)
        )

        // ── 본문: 완료 항목(위) + 현재 입력(아래) ────────────────────
        // 키보드가 올라와 가용 높이가 줄어도 입력 칸이 잘리지 않도록 스크롤 가능하게 둔다.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            // 완료된 항목 - 위에서 확장되며 등장 / 뒤로 가면 수축하며 사라짐
            AnimatedVisibility(
                visible = step > 0,
                enter = fadeIn(tween(300)) + expandVertically(tween(300, easing = FastOutSlowInEasing)),
                exit  = fadeOut(tween(220)) + shrinkVertically(tween(260, easing = FastOutSlowInEasing))
            ) { CompletedFieldRow(label = "이메일", value = email) }

            AnimatedVisibility(
                visible = step > 1,
                enter = fadeIn(tween(300)) + expandVertically(tween(300, easing = FastOutSlowInEasing)),
                exit  = fadeOut(tween(220)) + shrinkVertically(tween(260, easing = FastOutSlowInEasing))
            ) { CompletedFieldRow(label = "비밀번호", value = "입력 완료") }

            AnimatedVisibility(
                visible = step > 2,
                enter = fadeIn(tween(300)) + expandVertically(tween(300, easing = FastOutSlowInEasing)),
                exit  = fadeOut(tween(220)) + shrinkVertically(tween(260, easing = FastOutSlowInEasing))
            ) { CompletedFieldRow(label = "비밀번호 확인", value = "일치") }

            AnimatedVisibility(
                visible = step > 3,
                enter = fadeIn(tween(300)) + expandVertically(tween(300, easing = FastOutSlowInEasing)),
                exit  = fadeOut(tween(220)) + shrinkVertically(tween(260, easing = FastOutSlowInEasing))
            ) { CompletedFieldRow(label = "이름", value = name) }

            // 완료 항목과 현재 입력 사이 공간
            Spacer(Modifier.height(28.dp))

            // 현재 입력 필드 - 위로 슬라이드 아웃, 아래서 슬라이드 인
            AnimatedContent(
                targetState = step,
                modifier = Modifier.fillMaxWidth(),
                transitionSpec = {
                    val forward = targetState > initialState
                    if (forward) {
                        (slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 2 } +
                            fadeIn(tween(320, easing = FastOutSlowInEasing))) togetherWith
                        (slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { -it / 3 } +
                            fadeOut(tween(180)))
                    } else {
                        (slideInVertically(tween(380, easing = FastOutSlowInEasing)) { -it / 2 } +
                            fadeIn(tween(320, easing = FastOutSlowInEasing))) togetherWith
                        (slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it / 3 } +
                            fadeOut(tween(180)))
                    }
                },
                label = "signup_input"
            ) { s ->
                when (s) {
                    0 -> ActiveInputSection("이메일을\n입력해주세요", "로그인에 사용할 이메일 주소") {
                        Column {
                            InputField(
                                value = email,
                                onValueChange = { onEmailChange(it); emailError = null },
                                placeholder = "example@email.com",
                                keyboardType = KeyboardType.Email,
                                onDone = { advanceFromEmail() }
                            )
                            if (emailError != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(emailError!!, color = Color.Red, fontSize = 13.sp)
                            }
                        }
                    }
                    1 -> ActiveInputSection("비밀번호를\n설정해주세요", "6자 이상 입력해주세요") {
                        InputField(
                            value = password, onValueChange = onPasswordChange,
                            placeholder = "비밀번호 (6자 이상)",
                            keyboardType = KeyboardType.Password,
                            isPassword = true,
                            onDone = { if (isCurrentValid) onStepChange(2) }
                        )
                    }
                    2 -> ActiveInputSection("비밀번호를\n한 번 더 입력해주세요") {
                        Column {
                            InputField(
                                value = passwordConfirm, onValueChange = onPasswordConfirmChange,
                                placeholder = "비밀번호 확인",
                                keyboardType = KeyboardType.Password,
                                isPassword = true,
                                onDone = { if (isCurrentValid) onStepChange(3) }
                            )
                            if (passwordConfirm.isNotBlank() && passwordConfirm != password) {
                                Spacer(Modifier.height(8.dp))
                                Text("비밀번호가 일치하지 않습니다.", color = Color.Red, fontSize = 13.sp)
                            }
                        }
                    }
                    3 -> ActiveInputSection("이름을\n입력해주세요", "학생의 이름") {
                        InputField(
                            value = name, onValueChange = onNameChange,
                            placeholder = "이름을 입력하세요",
                            keyboardType = KeyboardType.Text,
                            onDone = { if (isCurrentValid) onStepChange(4) }
                        )
                    }
                    4 -> ActiveInputSection("생년월일을\n입력해주세요", "8자리 숫자 (예: 20001231)") {
                        InputField(
                            value = birthDate,
                            onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) onBirthDateChange(it) },
                            placeholder = "20001231",
                            keyboardType = KeyboardType.Number,
                            onDone = { if (isCurrentValid) onComplete() }
                        )
                    }
                    else -> {}
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── 다음 버튼 ────────────────────────────────────────────────
        Button(
            onClick = {
                if (!isCurrentValid) return@Button
                when (step) {
                    0 -> advanceFromEmail()
                    4 -> onComplete()
                    else -> onStepChange(step + 1)
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCurrentValid) themeColor else Color(0xFFCCCCCC),
                contentColor = Color.White
            ),
            enabled = isCurrentValid
        ) { Text("다음", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(Modifier.height(32.dp))
    }
}

// ── 완료된 항목 행 ────────────────────────────────────────────────────
@Composable
private fun CompletedFieldRow(label: String, value: String) {
    val themeColor = LocalThemeColor.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(22.dp).background(themeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check, contentDescription = null,
                tint = Color.White, modifier = Modifier.size(13.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.Gray, letterSpacing = 0.3.sp)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        }
    }
    HorizontalDivider(color = DividerColor, thickness = 1.dp)
}

// ── 현재 활성 입력 섹션 ────────────────────────────────────────────────
@Composable
private fun ActiveInputSection(
    title: String,
    subtitle: String = "",
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp, color = Color.Black)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(subtitle, fontSize = 14.sp, color = Color.Gray)
        }
        Spacer(Modifier.height(28.dp))
        content()
    }
}

// ── 공통 TextField ────────────────────────────────────────────────────
@Composable
private fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
    onDone: () -> Unit = {}
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = FieldBg, focusedContainerColor = FieldBg,
            unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent
        )
    )
}

