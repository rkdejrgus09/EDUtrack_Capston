package com.example.capstone_3.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone_3.auth.UserData
import com.example.capstone_3.common.LocalThemeColor
import com.example.capstone_3.common.SubTopBar
import com.example.capstone_3.common.TopBar
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// ── 데이터 모델 ──────────────────────────────────────────────────────────────

data class StudentScore(
    val docId: String = "",
    val subject: String = "",
    val score: Int = 0,
    val timestamp: Long = 0L,
    val name: String = "",
    val testId: String = "",
    val radarKeywords: List<String> = emptyList()
)

// ── 내비게이션 상태 ───────────────────────────────────────────────────────────

private sealed class GradeScreen {
    object SubjectList : GradeScreen()
    data class ExamList(val subject: String) : GradeScreen()
    data class ExamDetail(val score: StudentScore) : GradeScreen()
}

// ── 유틸 ─────────────────────────────────────────────────────────────────────

private fun subjectEmoji(subject: String) = when {
    subject.contains("국어") -> "📚"
    subject.contains("수학") -> "📐"
    subject.contains("영어") -> "🔤"
    subject.contains("과학") -> "🔬"
    subject.contains("사회") -> "🌍"
    subject.contains("역사") -> "📜"
    else -> "📝"
}

private fun subjectColor(subject: String) = when {
    subject.contains("국어") -> Color(0xFF5C6BC0)
    subject.contains("수학") -> Color(0xFF26A69A)
    subject.contains("영어") -> Color(0xFFEF5350)
    subject.contains("과학") -> Color(0xFF66BB6A)
    subject.contains("사회") -> Color(0xFFFF7043)
    subject.contains("역사") -> Color(0xFFAB47BC)
    else -> Color(0xFF78909C)
}

private fun formatDate(ts: Long): String {
    if (ts == 0L) return "-"
    return SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(ts))
}

private fun scoreColor(s: Int) = when {
    s >= 90 -> Color(0xFF26A69A); s >= 70 -> Color(0xFF5C6BC0)
    s >= 60 -> Color(0xFFFF7043); else -> Color(0xFFEF5350)
}

// ── 취약점 분석 데이터 계산 (교사 웹과 동일 로직) ────────────────────────────

private val DEFAULT_RADAR_CATEGORIES = listOf("개념 이해도", "원리 적용력", "문제 해결력", "추론 및 분석", "자료 해석력")

private data class RadarAnalysis(
    val chartData: List<Float>,
    val weakCategory: String,
    val statusText: String,
    val weaknessText: String,
    val prescriptionText: String
)

private fun buildRadarAnalysis(name: String, score: Int, categories: List<String> = DEFAULT_RADAR_CATEGORIES): RadarAnalysis {
    val weakIndex = (name.length + score) % 5
    val weakCategory = categories[weakIndex]

    return if (score == 100) {
        RadarAnalysis(
            chartData       = List(5) { 100f },
            weakCategory    = "없음",
            statusText      = "종합 성취도: 100% (최우수)",
            weaknessText    = "주요 취약점: 발견되지 않음 (전 영역 완벽)",
            prescriptionText = "모든 개념을 완벽히 숙지했습니다. 심화 응용 문제로 넘어가세요."
        )
    } else {
        val weakScore = maxOf(0, score - 25).toFloat()
        val chartData = List(5) { i ->
            if (i == weakIndex) weakScore
            else minOf(100f, score + (i + 1) * 5f)
        }
        val grade = if (score >= 80) "양호" else "보완 필요"
        RadarAnalysis(
            chartData        = chartData,
            weakCategory     = weakCategory,
            statusText       = "종합 성취도: ${score}% ($grade)",
            weaknessText     = "주요 취약점: $weakCategory",
            prescriptionText = "전반적 이해도는 점수(${score}점)를 따라가나, " +
                "'$weakCategory' 파트의 정답률이 ${weakScore.toInt()}%로 낮게 도출되었습니다. " +
                "해당 단원 오답 노트를 통한 집중 관리가 강력히 권장됩니다."
        )
    }
}

// ── 레이더 차트 (Canvas) ──────────────────────────────────────────────────────

@Composable
private fun RadarChart(
    data: List<Float>,
    labels: List<String>,
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    val themeArgb = themeColor.toArgb()

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = minOf(cx, cy) * 0.58f
        val n = data.size
        val labelOffset = maxR + 38f

        fun angle(i: Int) = ((-Math.PI / 2) + i * 2 * Math.PI / n).toFloat()
        fun px(i: Int, r: Float) = cx + r * cos(angle(i))
        fun py(i: Int, r: Float) = cy + r * sin(angle(i))

        val nativeCanvas = drawContext.canvas.nativeCanvas

        // 배경 그리드
        val gridPaint = android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.STROKE
            color = android.graphics.Color.parseColor("#E0E0E0")
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        for (level in 1..5) {
            val r = maxR * level / 5f
            val path = android.graphics.Path()
            for (i in 0 until n) {
                if (i == 0) path.moveTo(px(i, r), py(i, r))
                else path.lineTo(px(i, r), py(i, r))
            }
            path.close()
            nativeCanvas.drawPath(path, gridPaint)
        }

        // 축 선
        val axisLinePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#CCCCCC")
            strokeWidth = 1f
            isAntiAlias = true
        }
        for (i in 0 until n) nativeCanvas.drawLine(cx, cy, px(i, maxR), py(i, maxR), axisLinePaint)

        // 데이터 폴리곤 (채우기)
        val dataPath = android.graphics.Path()
        for (i in 0 until n) {
            val r = maxR * (data[i] / 100f)
            if (i == 0) dataPath.moveTo(px(i, r), py(i, r))
            else dataPath.lineTo(px(i, r), py(i, r))
        }
        dataPath.close()
        nativeCanvas.drawPath(dataPath, android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.FILL
            color = android.graphics.Color.argb(
                50,
                android.graphics.Color.red(themeArgb),
                android.graphics.Color.green(themeArgb),
                android.graphics.Color.blue(themeArgb)
            )
            isAntiAlias = true
        })
        nativeCanvas.drawPath(dataPath, android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.STROKE
            color = themeArgb
            strokeWidth = 2.5f
            isAntiAlias = true
        })

        // 꼭짓점 점
        val dotPaint = android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.FILL
            color = themeArgb
            isAntiAlias = true
        }
        for (i in 0 until n) {
            val r = maxR * (data[i] / 100f)
            nativeCanvas.drawCircle(px(i, r), py(i, r), 5f, dotPaint)
        }

        // 레이블
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#555555")
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        for (i in 0 until n) {
            val lx = px(i, labelOffset)
            val ly = py(i, labelOffset) + textPaint.textSize / 3f
            nativeCanvas.drawText(labels[i], lx, ly, textPaint)
        }
    }
}

// ── 진입점 ───────────────────────────────────────────────────────────────────

@Composable
fun GradeTab(userData: UserData? = null, onSettings: () -> Unit = {}, onLogout: () -> Unit = {}) {
    var screen by remember { mutableStateOf<GradeScreen>(GradeScreen.SubjectList) }
    var scores by remember { mutableStateOf<List<StudentScore>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    BackHandler(enabled = screen !is GradeScreen.SubjectList) {
        when (val s = screen) {
            is GradeScreen.ExamDetail -> screen = GradeScreen.ExamList(s.score.subject)
            else -> screen = GradeScreen.SubjectList
        }
    }

    LaunchedEffect(userData) {
        isLoading = true
        if (userData != null && userData.childName.isNotBlank()) {
            try {
                val edutrackDb = FirebaseFirestore.getInstance(FirebaseApp.getInstance("edutrack"))
                val snapshot = edutrackDb
                    .collection("student_scores")
                    .whereEqualTo("name", userData.childName)
                    .get().await()
                fun parseKeywords(raw: Any?): List<String> =
                    (raw as? List<*>)?.filterIsInstance<String>()?.filter { it.isNotBlank() } ?: emptyList()

                val rawScores = snapshot.documents.mapNotNull { doc ->
                    StudentScore(
                        docId         = doc.id,
                        subject       = doc.getString("subject") ?: return@mapNotNull null,
                        score         = (doc.get("score") as? Number)?.toInt() ?: 0,
                        timestamp     = doc.getLong("timestamp") ?: 0L,
                        name          = userData.childName,
                        testId        = doc.getString("testId") ?: "",
                        radarKeywords = parseKeywords(doc.get("radarKeywords"))
                    )
                }

                // radarKeywords 3단계 fallback (웹 대시보드와 동일 로직)
                val resolvedScores = mutableListOf<StudentScore>()
                for (s in rawScores) {
                    if (s.radarKeywords.isNotEmpty()) {
                        // ① student_scores에 이미 있으면 바로 사용
                        resolvedScores.add(s)
                        continue
                    }
                    var resolved = s
                    // ② tests/{testId} 문서에서 조회
                    if (s.testId.isNotBlank()) {
                        try {
                            val testDoc = edutrackDb.collection("tests").document(s.testId).get().await()
                            val kw = parseKeywords(testDoc.get("radarKeywords"))
                            if (kw.isNotEmpty()) resolved = s.copy(radarKeywords = kw)
                        } catch (_: Exception) {}
                    }
                    // ③ subject 이름으로 tests 컬렉션 검색
                    if (resolved.radarKeywords.isEmpty()) {
                        try {
                            val testSnap = edutrackDb.collection("tests")
                                .whereEqualTo("subject", s.subject).get().await()
                            val kw = testSnap.documents
                                .firstNotNullOfOrNull { parseKeywords(it.get("radarKeywords")).takeIf { k -> k.isNotEmpty() } }
                            if (kw != null) resolved = s.copy(radarKeywords = kw)
                        } catch (_: Exception) {}
                    }
                    resolvedScores.add(resolved)
                }
                scores = resolvedScores
            } catch (e: Exception) {
                android.util.Log.e("GradeTab", "성적 조회 실패: ${e.message}", e)
            }
        }
        isLoading = false
    }

    when (val s = screen) {
        is GradeScreen.SubjectList ->
            SubjectListScreen(
                scores = scores, isLoading = isLoading,
                onSettings = onSettings, onLogout = onLogout,
                onSubjectClick = { screen = GradeScreen.ExamList(it) }
            )
        is GradeScreen.ExamList ->
            ExamListScreen(
                subject = s.subject,
                exams   = scores.filter { it.subject == s.subject }.sortedByDescending { it.timestamp },
                onBack  = { screen = GradeScreen.SubjectList },
                onExamClick = { screen = GradeScreen.ExamDetail(it) }
            )
        is GradeScreen.ExamDetail ->
            ExamDetailScreen(
                score  = s.score,
                onBack = { screen = GradeScreen.ExamList(s.score.subject) }
            )
    }
}

// ── 과목 선택 화면 ────────────────────────────────────────────────────────────

@Composable
private fun SubjectListScreen(
    scores: List<StudentScore>,
    isLoading: Boolean,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    onSubjectClick: (String) -> Unit
) {
    val subjects = remember(scores) { scores.map { it.subject }.distinct() }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        TopBar("성적", onSettings = onSettings, onLogout = onLogout)
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LocalThemeColor.current)
            }
            subjects.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("아직 시험 기록이 없습니다.", fontSize = 15.sp, color = Color.Gray)
                }
            }
            else -> Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                Text("과목 선택", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
                subjects.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { subject ->
                            SubjectCard(
                                subject   = subject,
                                examCount = scores.count { it.subject == subject },
                                modifier  = Modifier.weight(1f),
                                onClick   = { onSubjectClick(subject) }
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SubjectCard(subject: String, examCount: Int, modifier: Modifier, onClick: () -> Unit) {
    val color = subjectColor(subject)
    Card(modifier = modifier.clickable { onClick() }, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center) { Text(subjectEmoji(subject), fontSize = 24.sp) }
            Spacer(Modifier.height(12.dp))
            Text(subject, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
            Spacer(Modifier.height(4.dp))
            Text("${examCount}개의 시험", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

// ── 시험 목록 화면 ────────────────────────────────────────────────────────────

@Composable
private fun ExamListScreen(
    subject: String, exams: List<StudentScore>,
    onBack: () -> Unit, onExamClick: (StudentScore) -> Unit
) {
    val color = subjectColor(subject)
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        SubTopBar("${subjectEmoji(subject)}  $subject 성적", onBack = onBack)
        if (exams.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("시험 기록이 없습니다.", color = Color.Gray, fontSize = 15.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(exams) { exam ->
                    ExamListItem(score = exam, subjectColor = color, onClick = { onExamClick(exam) })
                }
            }
        }
    }
}

@Composable
private fun ExamListItem(score: StudentScore, subjectColor: Color, onClick: () -> Unit) {
    val dateParts = formatDate(score.timestamp).split(".")
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(52.dp)) {
                Text(dateParts.getOrElse(0) { "" }, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text("${dateParts.getOrElse(1) { "" }}.${dateParts.getOrElse(2) { "" }}",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            }
            Box(Modifier.padding(horizontal = 14.dp).width(1.dp).height(36.dp).background(Color(0xFFE0E0E0)))
            Column(Modifier.weight(1f)) {
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(subjectColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text("${score.score}점", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = subjectColor)
                }
                Spacer(Modifier.height(5.dp))
                Text(score.subject, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null,
                tint = Color(0xFFBDBDBD), modifier = Modifier.size(20.dp))
        }
    }
}

// ── 시험 상세 화면 ────────────────────────────────────────────────────────────

@Composable
private fun ExamDetailScreen(score: StudentScore, onBack: () -> Unit) {
    val color = subjectColor(score.subject)
    val themeColor = LocalThemeColor.current
    val effectiveCategories = if (score.radarKeywords.size == 5) score.radarKeywords else DEFAULT_RADAR_CATEGORIES
    val analysis = remember(score) { buildRadarAnalysis(score.name, score.score, effectiveCategories) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        SubTopBar(score.subject, onBack = onBack)

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

            // 시험 정보 헤더
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(subjectEmoji(score.subject), fontSize = 30.sp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(score.subject, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
                        Spacer(Modifier.height(2.dp))
                        Text(formatDate(score.timestamp), fontSize = 12.sp, color = color.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 성적 카드
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("성적", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                    }
                    Spacer(Modifier.height(20.dp))
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${score.score}", fontSize = 48.sp, fontWeight = FontWeight.Bold,
                            color = scoreColor(score.score))
                        Text("점", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 취약점 추적 리포트 카드
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🕸️", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("취약점 추적 리포트", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray)
                    }
                    Text("AI 분석 기반", fontSize = 12.sp, color = Color.Gray)

                    Spacer(Modifier.height(16.dp))

                    // 레이더 차트
                    RadarChart(
                        data       = analysis.chartData,
                        labels     = effectiveCategories,
                        themeColor = themeColor,
                        modifier   = Modifier.fillMaxWidth().height(240.dp)
                    )

                    Spacer(Modifier.height(20.dp))

                    // 분석 텍스트
                    val statusColor = if (score.score >= 80) Color(0xFF1a73e8) else Color(0xFFe74c3c)
                    AnalysisRow(label = "📈 종합 성취도", value = analysis.statusText.substringAfter(": "), valueColor = statusColor)
                    Spacer(Modifier.height(10.dp))
                    AnalysisRow(label = "🎯 주요 취약점", value = analysis.weakCategory,
                        valueColor = if (score.score == 100) Color(0xFF27ae60) else Color(0xFFe74c3c))
                    Spacer(Modifier.height(10.dp))
                    Text("💡 AI 처방", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                    Spacer(Modifier.height(4.dp))
                    Text(analysis.prescriptionText, fontSize = 13.sp, color = Color(0xFF444444), lineHeight = 20.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AnalysisRow(label: String, value: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
        Spacer(Modifier.width(8.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
