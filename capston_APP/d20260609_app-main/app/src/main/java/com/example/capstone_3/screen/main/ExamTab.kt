package com.example.capstone_3.screen.main

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone_3.auth.UserData
import com.example.capstone_3.common.LocalThemeColor
import com.example.capstone_3.common.SubTopBar
import com.example.capstone_3.common.TopBar
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

private const val EXAM_BASE_URL = "https://edutrack-sable.vercel.app/exam"
private const val SHORT_URL_BASE = "https://edutrack-sable.vercel.app/e"

// ── 데이터 모델 ───────────────────────────────────────────────────────────────

data class DeployedExam(
    val docId: String = "",
    val testId: String = "",
    val subject: String = "",
    val grade: String = "",
    val classNum: String = "",
    val activatedAt: Long = 0L
)

// ── 내비게이션 상태 ───────────────────────────────────────────────────────────

private sealed class ExamNavState {
    object Main : ExamNavState()
    object Loading : ExamNavState()
    data class ExamList(
        val exams: List<DeployedExam>,
        val submittedTestIds: Set<String>
    ) : ExamNavState()
    // 선택된 시험의 QR+링크 화면. allExams/submittedTestIds는 뒤로가기 복원용.
    // url은 state에 저장하지 않고 ExamEntryScreen 내부에서 항상 새로 계산.
    data class ExamEntry(
        val exam: DeployedExam,
        val allExams: List<DeployedExam>,
        val submittedTestIds: Set<String>
    ) : ExamNavState()
}

// ── 유틸 ─────────────────────────────────────────────────────────────────────

private fun buildExamUrl(exam: DeployedExam, userData: UserData): String =
    Uri.parse(EXAM_BASE_URL).buildUpon()
        .appendQueryParameter("testId", exam.testId)
        .appendQueryParameter("grade", exam.grade)
        .appendQueryParameter("class", exam.classNum)
        .appendQueryParameter("name", userData.childName)
        .apply {
            // 번호까지 전달해야 시험방에서 동명이인 구분 + 번호 입력 없이 자동 입장됨
            if (userData.childNumber > 0) appendQueryParameter("number", userData.childNumber.toString())
        }
        .build()
        .toString()

private fun formatActivatedAt(ts: Long): String {
    if (ts == 0L) return ""
    return SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date(ts))
}

private fun generateQrBitmap(url: String, size: Int = 512): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, size, size, hints)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bmp
}

private suspend fun getOrCreateShortCode(exam: DeployedExam, userData: UserData): String {
    val db = FirebaseFirestore.getInstance(FirebaseApp.getInstance("edutrack"))
    val lookupKey = "${exam.testId}_${exam.grade}_${exam.classNum}_${userData.childName}"

    val lookupSnap = db.collection("short_link_lookups").document(lookupKey).get().await()
    if (lookupSnap.exists()) {
        lookupSnap.getString("shortCode")?.let { existingCode ->
            // 기존 short_links 문서에 number 업데이트 (번호 시스템 추가 후 재반영)
            if (userData.childNumber > 0) {
                db.collection("short_links").document(existingCode)
                    .update(mapOf("number" to userData.childNumber)).await()
            }
            return existingCode
        }
    }

    val chars = (('A'..'Z') + ('0'..'9')).toList()
    var code: String
    var attempts = 0
    do {
        code = (1..6).map { chars.random() }.joinToString("")
        attempts++
    } while (attempts < 5 && db.collection("short_links").document(code).get().await().exists())

    db.collection("short_links").document(code).set(mapOf(
        "grade" to exam.grade,
        "class" to exam.classNum,
        "name" to userData.childName,
        "number" to userData.childNumber,
        "testId" to exam.testId,
        "createdAt" to System.currentTimeMillis()
    )).await()
    db.collection("short_link_lookups").document(lookupKey).set(mapOf("shortCode" to code)).await()

    return code
}

private suspend fun loadDeployedExams(userData: UserData): Pair<List<DeployedExam>, Set<String>> {
    return try {
        val db = FirebaseFirestore.getInstance(FirebaseApp.getInstance("edutrack"))
        val gradeClass = "${userData.childGrade}_${userData.childClass}"

        val deploySnap = db.collection("deployments")
            .whereEqualTo("gradeClass", gradeClass)
            .get().await()

        val activeExams = deploySnap.documents
            .filter { it.getString("status") == "active" }
            .mapNotNull { doc ->
                DeployedExam(
                    docId       = doc.id,
                    testId      = doc.getString("testId") ?: return@mapNotNull null,
                    subject     = doc.getString("subject") ?: "",
                    grade       = doc.getString("grade") ?: userData.childGrade.toString(),
                    classNum    = doc.getString("class") ?: userData.childClass.toString(),
                    activatedAt = doc.getLong("activatedAt") ?: 0L
                )
            }

        val sessionSnap = db.collection("exam_sessions")
            .whereEqualTo("name", userData.childName)
            .whereEqualTo("status", "submitted")
            .get().await()

        val submittedTestIds = sessionSnap.documents
            .mapNotNull { it.getString("testId") }
            .toSet()

        Pair(activeExams, submittedTestIds)
    } catch (_: Exception) {
        Pair(emptyList(), emptySet())
    }
}

// ── 진입점 ───────────────────────────────────────────────────────────────────

@Composable
fun ExamTab(userData: UserData? = null, onSettings: () -> Unit = {}, onLogout: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var navState by remember { mutableStateOf<ExamNavState>(ExamNavState.Main) }

    BackHandler(enabled = navState !is ExamNavState.Main) {
        when (val state = navState) {
            is ExamNavState.ExamEntry -> navState = if (state.allExams.size > 1)
                ExamNavState.ExamList(state.allExams, state.submittedTestIds)
            else
                ExamNavState.Main
            else -> navState = ExamNavState.Main
        }
    }

    when (val state = navState) {
        is ExamNavState.Main ->
            ExamMainScreen(
                onSettings = onSettings,
                onLogout   = onLogout,
                onEnter    = {
                    if (userData != null) {
                        navState = ExamNavState.Loading
                        scope.launch {
                            val (exams, submittedIds) = loadDeployedExams(userData)
                            navState = when {
                                // 시험이 1개이고 미제출이면 목록 생략, 바로 QR+링크 화면
                                exams.size == 1 && exams[0].testId !in submittedIds ->
                                    ExamNavState.ExamEntry(
                                        exam             = exams[0],
                                        allExams         = exams,
                                        submittedTestIds = submittedIds
                                    )
                                else ->
                                    ExamNavState.ExamList(exams, submittedIds)
                            }
                        }
                    }
                }
            )

        is ExamNavState.Loading ->
            Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F2))) {
                TopBar("시험", onSettings = onSettings, onLogout = onLogout)
                Box(
                    modifier         = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LocalThemeColor.current)
                }
            }

        is ExamNavState.ExamList ->
            ExamListScreen(
                exams            = state.exams,
                submittedTestIds = state.submittedTestIds,
                onBack           = { navState = ExamNavState.Main },
                onExamClick      = { exam ->
                    if (userData != null) {
                        navState = ExamNavState.ExamEntry(
                            exam             = exam,
                            allExams         = state.exams,
                            submittedTestIds = state.submittedTestIds
                        )
                    }
                }
            )

        is ExamNavState.ExamEntry -> {
            // docId를 key로 지정 → 시험이 바뀔 때 ExamEntryScreen 완전 재생성
            // userData는 null일 수 없음 (ExamEntry 진입 시 null 체크 완료)
            key(state.exam.docId) {
                ExamEntryScreen(
                    exam     = state.exam,
                    userData = userData!!,
                    onBack   = {
                        navState = if (state.allExams.size > 1)
                            ExamNavState.ExamList(state.allExams, state.submittedTestIds)
                        else
                            ExamNavState.Main
                    }
                )
            }
        }
    }
}

// ── 시험 메인 화면 ────────────────────────────────────────────────────────────

@Composable
private fun ExamMainScreen(onSettings: () -> Unit, onLogout: () -> Unit, onEnter: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar("시험", onSettings = onSettings, onLogout = onLogout)
        Box(
            modifier         = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint     = LocalThemeColor.current.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(36.dp))
                Button(
                    onClick  = onEnter,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = LocalThemeColor.current)
                ) {
                    Text("시험 보러가기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── 시험 목록 화면 ────────────────────────────────────────────────────────────

@Composable
private fun ExamListScreen(
    exams: List<DeployedExam>,
    submittedTestIds: Set<String>,
    onBack: () -> Unit,
    onExamClick: (DeployedExam) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F2))) {
        SubTopBar("시험 목록", onBack = onBack)
        val pendingExams   = exams.filter { it.testId !in submittedTestIds }
        val submittedExams = exams.filter { it.testId in submittedTestIds }

        if (exams.isEmpty()) {
            Box(
                modifier         = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("현재 배포된 시험이 없습니다.", fontSize = 15.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.weight(1f).fillMaxWidth(),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pendingExams.isNotEmpty()) {
                    item {
                        Text(
                            "응시 가능한 시험",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color(0xFF888888),
                            modifier   = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                    items(pendingExams, key = { it.docId }) { exam ->
                        ExamCard(
                            exam        = exam,
                            isSubmitted = false,
                            onClick     = { onExamClick(exam) }
                        )
                    }
                }

                if (submittedExams.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(if (pendingExams.isNotEmpty()) 8.dp else 0.dp))
                        Text(
                            "제출 완료된 시험",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color(0xFF888888),
                            modifier   = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                    items(submittedExams, key = { it.docId }) { exam ->
                        ExamCard(
                            exam        = exam,
                            isSubmitted = true,
                            onClick     = {}
                        )
                    }
                }
            }
        }
    }
}

// ── 시험 카드 ─────────────────────────────────────────────────────────────────

@Composable
private fun ExamCard(exam: DeployedExam, isSubmitted: Boolean, onClick: () -> Unit) {
    val themeColor = LocalThemeColor.current
    Card(
        modifier  = Modifier.fillMaxWidth().clickable(enabled = !isSubmitted) { onClick() },
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isSubmitted) Color(0xFFF5F5F5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(if (isSubmitted) 0.dp else 2.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSubmitted) Color(0xFFE0E0E0) else themeColor.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("📝", fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    exam.subject.ifBlank { "시험" },
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (isSubmitted) Color.Gray else Color.DarkGray
                )
                if (exam.activatedAt > 0L) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "배포: ${formatActivatedAt(exam.activatedAt)}",
                        fontSize = 12.sp,
                        color    = Color(0xFFBDBDBD)
                    )
                }
            }
            if (isSubmitted) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E0E0))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("제출 완료", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            } else {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint     = themeColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ── QR + 링크 화면 ────────────────────────────────────────────────────────────

@Composable
private fun ExamEntryScreen(exam: DeployedExam, userData: UserData, onBack: () -> Unit) {
    val context    = LocalContext.current
    val themeColor = LocalThemeColor.current
    var shortUrl by remember(exam.docId) { mutableStateOf<String?>(null) }

    LaunchedEffect(exam.docId) {
        val code = getOrCreateShortCode(exam, userData)
        shortUrl = "$SHORT_URL_BASE/$code"
    }

    val qrBitmap = remember(shortUrl) { shortUrl?.let { generateQrBitmap(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F2))
    ) {
        SubTopBar(exam.subject.ifBlank { "시험 보러가기" }, onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // QR 코드 카드
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "QR 코드",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.DarkGray,
                        modifier   = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "QR 코드를 스캔하여 시험에 접속하세요.",
                        fontSize = 12.sp,
                        color    = Color.Gray,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(20.dp))
                    if (qrBitmap != null) {
                        Image(
                            bitmap             = qrBitmap.asImageBitmap(),
                            contentDescription = "시험 QR 코드",
                            modifier           = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(
                            modifier         = Modifier.size(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = themeColor)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 링크 카드
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(
                        "링크",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.DarkGray
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "링크를 눌러 시험 페이지로 이동할 수 있습니다.",
                        fontSize = 12.sp,
                        color    = Color.Gray
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, themeColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .background(themeColor.copy(alpha = 0.05f))
                            .clickable {
                                shortUrl?.let {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint     = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            shortUrl ?: "링크 생성 중...",
                            fontSize = 13.sp,
                            color    = if (shortUrl != null) themeColor else Color.Gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
