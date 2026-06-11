package com.example.capstone_3.screen.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone_3.common.GreenTag
import com.example.capstone_3.common.LocalThemeColor
import com.example.capstone_3.common.LightGray

// step 0: 이메일 입력
// step 1: 전화번호 + 인증요청
// step 2: 인증번호 입력
// step 3: 새 비밀번호 재설정
@Composable
fun ForgotPasswordScreen(onBackToLogin: () -> Unit) {
    var step         by remember { mutableStateOf(0) }
    var email        by remember { mutableStateOf("") }
    var phone        by remember { mutableStateOf("") }
    var code         by remember { mutableStateOf("") }
    var newPw        by remember { mutableStateOf("") }
    var newPwConfirm by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth(0.88f).wrapContentHeight(),
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("에듀트랙", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = LocalThemeColor.current)
                Spacer(Modifier.height(6.dp))
                Text("비밀번호 찾기", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(20.dp))

                if (step == 3) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null,
                            tint = GreenTag, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("인증이 완료되었습니다", fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold, color = GreenTag)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("새로운 비밀번호를 입력해주세요.", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(20.dp))

                    TextField(
                        value = newPw, onValueChange = { newPw = it },
                        placeholder = { Text("새 비밀번호", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = LightGray, focusedContainerColor = LightGray,
                            unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = newPwConfirm, onValueChange = { newPwConfirm = it },
                        placeholder = { Text("새 비밀번호 확인", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = LightGray, focusedContainerColor = LightGray,
                            unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent
                        )
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { onBackToLogin() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalThemeColor.current)
                    ) { Text("비밀번호 변경하기", fontWeight = FontWeight.Bold) }

                } else {
                    Text(
                        "가입하신 이메일 주소를 입력하시면 비밀번호를 재설정할 수 있습니다.",
                        fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))

                    TextField(
                        value = email, onValueChange = { if (step == 0) email = it },
                        placeholder = { Text("이메일 주소", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                        enabled = step == 0,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = LightGray, focusedContainerColor = LightGray,
                            disabledContainerColor = LightGray,
                            unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )

                    if (step >= 1) {
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = phone, onValueChange = { if (step == 1) phone = it },
                                placeholder = { Text("010-0000-0000", color = Color.Gray) },
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true,
                                enabled = step == 1,
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = LightGray, focusedContainerColor = LightGray,
                                    disabledContainerColor = LightGray,
                                    unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { if (step == 1) step = 2 },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (step == 1) LocalThemeColor.current else Color(0xFFCCCCCC)
                                ),
                                enabled = step == 1
                            ) { Text(if (step == 1) "인증요청" else "전송완료", fontSize = 13.sp) }
                        }
                    }

                    if (step >= 2) {
                        Spacer(Modifier.height(8.dp))
                        TextField(
                            value = code, onValueChange = { code = it },
                            placeholder = { Text("인증번호 입력", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = LightGray, focusedContainerColor = LightGray,
                                unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent
                            )
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            when (step) {
                                0 -> if (email.isNotBlank()) step = 1
                                2 -> if (code.isNotBlank()) step = 3
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (step == 1) Color(0xFFCCCCCC) else LocalThemeColor.current
                        ),
                        enabled = step != 1
                    ) {
                        Text(
                            text = when (step) {
                                0    -> "이메일 중복 확인"
                                2    -> "비밀번호 재설정하기"
                                else -> "이메일 중복 확인"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("로그인으로 돌아가기", fontSize = 13.sp, color = Color.Gray,
                    modifier = Modifier.clickable { onBackToLogin() })
            }
        }
    }
}
