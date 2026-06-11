package com.example.capstone_3.screen.auth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.capstone_3.R
import com.example.capstone_3.auth.AuthState
import com.example.capstone_3.auth.AuthViewModel
import com.example.capstone_3.common.LocalThemeColor
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

// ── 데이터 모델 ───────────────────────────────────────────────────────
data class SchoolInfo(
    val name: String,
    val address: String,
    val city: String
)

// ── 학교 레포지토리 (임시 하드코딩) ──────────────────────────────────
// NEIS API 연동 전까지 임시 데이터로 동작합니다.
object SchoolRepository {
    var lastError: String = ""

    // 임시 등록 학교 목록
    private val schools = listOf(
        SchoolInfo(
            name    = "경동고등학교",
            address = "서울특별시 성북구 종암로 135",
            city    = "서울특별시"
        )
    )

    suspend fun byName(query: String): List<SchoolInfo> {
        lastError = ""
        val q = query.trim()
        return schools.filter { it.name.contains(q) }
    }

    suspend fun byCity(city: String): List<SchoolInfo> {
        lastError = ""
        return schools.filter { it.city.contains(city) || city.contains(it.city) }
    }
}

// ── 위치 상태 ─────────────────────────────────────────────────────────
private sealed class LocState {
    object Idle    : LocState()
    object Loading : LocState()
    data class Ok(val city: String) : LocState()
    object Denied  : LocState()
    object NoFix   : LocState()
}

// ── 학교 + 학년·반 + 학급코드 통합 화면 (진행 단계 6/6) ──────────────
// 학교를 선택하면 학년·반 피커가, 학년·반을 확정하면 학급코드 입력이 차례로 펼쳐집니다.
@Composable
fun SchoolGradeCodeScreen(
    viewModel: AuthViewModel,
    email: String,
    password: String,
    birthDate: String,
    childName: String,
    initialGrade: Int,
    initialClass: Int,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val context    = LocalContext.current
    val themeColor = LocalThemeColor.current
    val scope      = rememberCoroutineScope()
    val authState  by viewModel.authState.collectAsState()

    // 학교 검색 상태
    var query    by remember { mutableStateOf("") }
    var schools  by remember { mutableStateOf<List<SchoolInfo>>(emptyList()) }
    var loading  by remember { mutableStateOf(false) }
    var locState by remember { mutableStateOf<LocState>(LocState.Idle) }

    // 단계별 진행 상태
    var selectedSchool    by remember { mutableStateOf<SchoolInfo?>(null) }
    var gradeIndex        by remember { mutableIntStateOf(initialGrade - 1) }
    var classIndex        by remember { mutableIntStateOf(initialClass - 1) }
    var gradeConfirmed    by remember { mutableStateOf(false) }
    var studentNumber     by remember { mutableStateOf("") }
    var classCode         by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // 학년·반 선택 팝업 상태 (확인 전까지는 임시값만 변경)
    var showGradeDialog   by remember { mutableStateOf(false) }
    var tempGrade         by remember { mutableIntStateOf(initialGrade - 1) }
    var tempClass         by remember { mutableIntStateOf(initialClass - 1) }

    val grades  = (1..3).map { "${it}학년" }
    val classes = (1..15).map { "${it}반" }

    val isLoading    = authState is AuthState.Loading
    val errorMessage = (authState as? AuthState.Error)?.message
    val numberValid  = studentNumber.toIntOrNull()?.let { it in 1..40 } ?: false
    val codeValid    = classCode.length in 6..12

    // ── 위치 권한 런처 ────────────────────────────────────────────────
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            scope.launch {
                loadNearby(context, onState = { locState = it }, onSchools = { schools = it })
            }
        } else {
            locState = LocState.Denied
        }
    }

    // 이미 권한 있으면 자동 로드
    LaunchedEffect(Unit) {
        if (hasLocPerm(context)) {
            loadNearby(context, onState = { locState = it }, onSchools = { schools = it })
        }
    }

    // 검색어 디바운스
    LaunchedEffect(query) {
        if (query.length >= 2) {
            delay(420)
            loading = true
            schools = SchoolRepository.byName(query)
            loading = false
        } else if (query.isEmpty() && locState is LocState.Ok) {
            schools = SchoolRepository.byCity((locState as LocState.Ok).city)
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) showSuccessDialog = true
    }

    // 단계별 뒤로가기: 코드 → 학년·반 → 학교 검색 → 이전 화면
    val goBack: () -> Unit = {
        when {
            gradeConfirmed         -> gradeConfirmed = false
            selectedSchool != null -> { selectedSchool = null; classCode = "" }
            else                   -> { viewModel.resetState(); onBack() }
        }
    }
    BackHandler { goBack() }

    fun submit() {
        if (codeValid && numberValid && !isLoading) {
            viewModel.setPendingSignUpData(
                email, password, selectedSchool!!.name,
                gradeIndex + 1, classIndex + 1, childName, birthDate,
                studentNumber.toIntOrNull() ?: 0
            )
            viewModel.signUpWithClassCode(classCode)
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = Color.White,
            title = { Text("회원가입 완료", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = { Text("회원가입이 완료되었습니다!\n앱에 로그인하여 이용하세요.", color = Color.DarkGray, lineHeight = 22.sp) },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false; viewModel.resetState(); onComplete() },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("로그인 하러 가기", color = Color.White) }
            }
        )
    }

    // ── 학년·반 선택 팝업 ─────────────────────────────────────────────
    if (showGradeDialog) {
        AlertDialog(
            onDismissRequest = { showGradeDialog = false },
            containerColor = Color.White,
            title = { Text("학년과 반 선택", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("학년", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                        WheelPicker(
                            items = grades, selectedIndex = tempGrade,
                            onSelectedChange = { tempGrade = it },
                            visibleItemCount = 5, itemHeight = 44.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(180.dp).padding(top = 28.dp).background(Color(0xFFEEEEEE)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("반", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                        WheelPicker(
                            items = classes, selectedIndex = tempClass,
                            onSelectedChange = { tempClass = it },
                            visibleItemCount = 5, itemHeight = 44.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        gradeIndex = tempGrade; classIndex = tempClass
                        gradeConfirmed = true; showGradeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("확인", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showGradeDialog = false }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .imePadding()
    ) {
        // ── 헤더 (진행률) ────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = goBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "뒤로", tint = Color.DarkGray)
                }
                Spacer(Modifier.weight(1f))
                Text("7 / 7", fontSize = 13.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = themeColor, trackColor = Color(0xFFE0E0E0)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                if (selectedSchool == null) "재학 중인 학교를\n선택해주세요" else "학년·반과\n학급 코드를 입력해주세요",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp
            )
            Spacer(Modifier.height(20.dp))
        }

        if (selectedSchool == null) {
            // ── 1단계: 학교 검색 ────────────────────────────────────────
            SearchTab(
                query        = query,
                onQuery      = { query = it },
                schools      = schools,
                loading      = loading,
                locState     = locState,
                themeColor   = themeColor,
                onLocRequest = {
                    permLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                },
                onSelect = { selectedSchool = it; gradeConfirmed = false; classCode = "" }
            )
        } else {
            // ── 2·3단계: 선택한 학교 + 학년·반 + 학급코드 ───────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                // 선택한 학교
                SelectedSchoolCard(selectedSchool!!, themeColor) {
                    selectedSchool = null; gradeConfirmed = false; classCode = ""
                }

                Spacer(Modifier.height(24.dp))
                Text("학년과 반", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                // 탭하면 팝업으로 학년·반 선택, 선택 후 값이 표시됨
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .clickable {
                            tempGrade = gradeIndex; tempClass = classIndex
                            showGradeDialog = true
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (gradeConfirmed) "${gradeIndex + 1}학년 ${classIndex + 1}반" else "학년과 반을 선택하세요",
                        color = if (gradeConfirmed) Color.Black else Color.Gray,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                }

                if (gradeConfirmed) {
                    // ── 3단계: 학급 번호 ────────────────────────────────
                    Spacer(Modifier.height(28.dp))
                    Text("학급 번호", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("출석부 기준 번호 (1~40)", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    TextField(
                        value = studentNumber,
                        onValueChange = {
                            if (it.length <= 2 && it.all { c -> c.isDigit() }) studentNumber = it
                        },
                        placeholder = { Text("예: 15", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedContainerColor   = Color(0xFFF5F5F5),
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor   = Color.Transparent
                        )
                    )

                    // ── 4단계: 학급 코드 ────────────────────────────────
                    Spacer(Modifier.height(28.dp))
                    Text("학급 코드", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("담임 선생님께 받은 6~12자리 코드", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    TextField(
                        value = classCode,
                        onValueChange = {
                            if (it.length <= 12) {
                                classCode = it.uppercase().filter { c -> c.isLetterOrDigit() }
                                viewModel.resetState()
                            }
                        },
                        placeholder = { Text("KDHIGH12", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedContainerColor   = Color(0xFFF5F5F5),
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor   = Color.Transparent
                        )
                    )
                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorMessage, color = Color.Red, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { submit() },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (codeValid && numberValid && !isLoading) themeColor else Color(0xFFCCCCCC),
                            contentColor = Color.White
                        ),
                        enabled = codeValid && numberValid && !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        else Text("가입 완료", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// ── 학교 로고/이미지 ──────────────────────────────────────────────────
// 등록된 학교 이미지가 있으면 이미지를, 없으면 학사모 아이콘을 보여줍니다.
private fun schoolImageRes(name: String): Int? = when {
    name.contains("경동고") -> R.drawable.gyeongdong_high
    else -> null
}

@Composable
private fun SchoolLogo(school: SchoolInfo, themeColor: Color, boxSize: Dp, iconSize: Dp) {
    val imageRes = schoolImageRes(school.name)
    Box(
        modifier = Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(10.dp))
            .then(if (imageRes == null) Modifier.background(themeColor.copy(alpha = 0.1f)) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (imageRes != null) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = school.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(Icons.Default.School, contentDescription = null, tint = themeColor, modifier = Modifier.size(iconSize))
        }
    }
}

// ── 선택한 학교 카드 ──────────────────────────────────────────────────
@Composable
private fun SelectedSchoolCard(school: SchoolInfo, themeColor: Color, onChange: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFFF5F8FF)),
        border   = BorderStroke(1.dp, themeColor.copy(alpha = 0.35f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SchoolLogo(school, themeColor, boxSize = 44.dp, iconSize = 24.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(school.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                if (school.address.isNotBlank())
                    Text(school.address, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onChange, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("변경", fontSize = 13.sp, color = themeColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── 검색 탭 ───────────────────────────────────────────────────────────
@Composable
private fun SearchTab(
    query: String, onQuery: (String) -> Unit,
    schools: List<SchoolInfo>,
    loading: Boolean,
    locState: LocState,
    themeColor: Color,
    onLocRequest: () -> Unit,
    onSelect: (SchoolInfo) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 검색 입력창
        TextField(
            value = query, onValueChange = onQuery,
            placeholder = { Text("학교 이름으로 검색", color = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            leadingIcon  = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "지우기", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF5F5F5),
                focusedContainerColor   = Color(0xFFF5F5F5),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor   = Color.Transparent
            )
        )

        // 위치 배너
        if (query.isEmpty()) {
            when (locState) {
                LocState.Idle -> LocBanner(
                    icon = Icons.Default.LocationOn,
                    text = "위치를 허용하면 주변 고등학교를 바로 볼 수 있어요",
                    iconTint = Color(0xFF888888),
                    actionLabel = "허용하기", themeColor = themeColor,
                    onAction = onLocRequest
                )
                LocState.Loading -> LocBanner(
                    icon = Icons.Default.GpsFixed,
                    text = "현재 위치를 확인하는 중이에요...",
                    iconTint = themeColor, themeColor = themeColor
                )
                is LocState.Ok -> LocBanner(
                    icon = Icons.Default.LocationOn,
                    text = "${locState.city} 주변 고등학교",
                    iconTint = themeColor, themeColor = themeColor
                )
                LocState.NoFix -> LocBanner(
                    icon = Icons.Default.GpsOff,
                    text = "위치를 가져올 수 없어요. 학교 이름으로 검색해주세요.",
                    iconTint = Color(0xFFE57373), themeColor = themeColor
                )
                LocState.Denied -> LocBanner(
                    icon = Icons.Default.LocationOff,
                    text = "위치 권한이 거부됐어요. 학교 이름으로 검색해주세요.",
                    iconTint = Color(0xFFE57373), themeColor = themeColor
                )
            }
        }

        // 결과
        when {
            loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = themeColor, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
            schools.isNotEmpty() -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(schools, key = { it.name }) { school ->
                    SchoolItem(school, themeColor) { onSelect(school) }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
            query.length >= 2 && SchoolRepository.lastError.isNotBlank() ->
                Empty("네트워크 오류: ${SchoolRepository.lastError}")
            query.length >= 2 -> Empty("'$query'에 해당하는 고등학교가 없어요.")
            else -> Empty("학교 이름을 2글자 이상 입력해주세요.")
        }
    }
}

// ── 위치 배너 ─────────────────────────────────────────────────────────
@Composable
private fun LocBanner(
    icon: ImageVector,
    text: String,
    iconTint: Color,
    themeColor: Color,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.weight(1f), lineHeight = 18.sp)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(actionLabel, fontSize = 12.sp, color = themeColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── 학교 목록 아이템 ──────────────────────────────────────────────────
@Composable
private fun SchoolItem(school: SchoolInfo, themeColor: Color, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SchoolLogo(school, themeColor, boxSize = 42.dp, iconSize = 22.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(school.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            if (school.address.isNotBlank()) {
                Text(
                    school.address, fontSize = 12.sp, color = Color.Gray,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(20.dp))
    }
    HorizontalDivider(color = Color(0xFFF2F2F2), modifier = Modifier.padding(start = 70.dp))
}

// ── 빈 상태 ───────────────────────────────────────────────────────────
@Composable
private fun Empty(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 56.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color(0xFFDDDDDD), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(message, fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

// ── 위치 헬퍼 ─────────────────────────────────────────────────────────
private fun hasLocPerm(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private suspend fun loadNearby(
    context: Context,
    onState: (LocState) -> Unit,
    onSchools: (List<SchoolInfo>) -> Unit
) {
    onState(LocState.Loading)
    val city = getCity(context)
    if (city != null) {
        onState(LocState.Ok(city))
        onSchools(SchoolRepository.byCity(city))
    } else {
        onState(LocState.NoFix)
    }
}

private suspend fun getCity(context: Context): String? = withContext(Dispatchers.IO) {
    try {
        val client   = LocationServices.getFusedLocationProviderClient(context)
        val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
            client.lastLocation
                .addOnSuccessListener  { cont.resume(it) }
                .addOnFailureListener  { cont.resume(null) }
                .addOnCanceledListener { cont.resume(null) }
        } ?: return@withContext null

        val geocoder = Geocoder(context, Locale.KOREA)
        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(location.latitude, location.longitude, 1) { cont.resume(it) }
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
        }
        addresses?.firstOrNull()?.adminArea
    } catch (_: Exception) { null }
}
