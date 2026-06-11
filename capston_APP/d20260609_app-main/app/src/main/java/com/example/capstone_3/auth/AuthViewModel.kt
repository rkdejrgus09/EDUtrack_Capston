package com.example.capstone_3.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

data class UserData(
    val email: String = "",
    val childName: String = "",
    val childSchool: String = "",
    val childGrade: Int = 0,
    val childClass: Int = 0,
    val childNumber: Int = 0,
    val role: String = "",
    val birthDate: String = ""
) {
    val isAdmin: Boolean get() = role == "admin"
}

class AuthViewModel : ViewModel() {
    companion object {
        // 임시 하드코딩 학급 코드 (Firestore 연동 전까지 사용)
        private const val TEMP_CLASS_CODE = "KDHIGH12"
    }

    private val auth        = FirebaseAuth.getInstance()
    private val db          = FirebaseFirestore.getInstance()
    private val edutrackDb  = FirebaseFirestore.getInstance(FirebaseApp.getInstance("edutrack"))

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                loadUserData(currentUser.uid)
                _authState.value = AuthState.Success
            }
        }
    }

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData

    // 회원가입 입력 데이터
    private var pendingEmail        = ""
    private var pendingPassword     = ""
    private var pendingChildSchool  = ""
    private var pendingChildGrade   = 0
    private var pendingChildClass   = 0
    private var pendingChildNumber  = 0
    private var pendingName         = ""
    private var pendingBirthDate    = ""

    fun setPendingSignUpData(
        email: String, password: String,
        childSchool: String, childGrade: Int, childClass: Int,
        childName: String, birthDate: String, childNumber: Int
    ) {
        pendingEmail        = email
        pendingPassword     = password
        pendingChildSchool  = childSchool
        pendingChildGrade   = childGrade
        pendingChildClass   = childClass
        pendingChildNumber  = childNumber
        pendingName         = childName
        pendingBirthDate    = birthDate
    }

    // ─── 관리자 로그인 ────────────────────────────────────────────────
    private fun loginAsAdmin() {
        _userData.value = UserData(email = "admin", childName = "관리자", role = "admin")
        _authState.value = AuthState.Success
    }

    // ─── 일반 로그인 ──────────────────────────────────────────────────
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("이메일과 비밀번호를 입력해주세요.")
            return
        }
        if (email.trim() == "admin" && password == "admin") { loginAsAdmin(); return }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
                val uid = result.user?.uid
                if (uid != null) loadUserData(uid)
                _authState.value = AuthState.Success
            } catch (e: FirebaseAuthException) {
                _authState.value = AuthState.Error(mapAuthError(e.errorCode))
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapGenericError(e))
            }
        }
    }

    // ─── 학급 코드 검증 후 회원가입 ──────────────────────────────────
    fun signUpWithClassCode(code: String) {
        val trimmed = code.trim().uppercase()
        if (trimmed.length !in 6..12) {
            _authState.value = AuthState.Error("6~12자리 학급 코드를 입력해주세요.")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // 임시 하드코딩 학급 코드 (Firestore 연동 전까지 동작)
                if (trimmed == TEMP_CLASS_CODE) {
                    doSignUp(trimmed)
                    return@launch
                }
                val codeDoc = edutrackDb.collection("class_codes").document(trimmed).get().await()
                if (!codeDoc.exists()) {
                    _authState.value = AuthState.Error("존재하지 않는 코드입니다.")
                    return@launch
                }
                val data     = codeDoc.data!!
                val isActive = data["isActive"] as? Boolean ?: false
                val expires  = data["expiresAt"] as? Long ?: 0L
                if (!isActive) {
                    _authState.value = AuthState.Error("비활성화된 코드입니다.")
                    return@launch
                }
                if (expires > 0 && expires < System.currentTimeMillis()) {
                    _authState.value = AuthState.Error("기간이 만료된 코드입니다.")
                    return@launch
                }
                // 코드에서 학년·반 자동 설정 (학생 자가 입력보다 코드 우선)
                (data["grade"] as? String)?.toIntOrNull()?.let { pendingChildGrade = it }
                (data["class"] as? String)?.toIntOrNull()?.let { pendingChildClass = it }

                doSignUp(trimmed)
            } catch (_: Exception) {
                _authState.value = AuthState.Error("코드 확인 중 오류가 발생했습니다.")
            }
        }
    }

    private suspend fun doSignUp(classCode: String = "") {
        _authState.value = AuthState.Loading
        try {
            val result = auth.createUserWithEmailAndPassword(pendingEmail.trim(), pendingPassword).await()
            val uid    = result.user?.uid
            if (uid != null) {
                try {
                    db.collection("users").document(uid).set(
                        mapOf(
                            "email"        to pendingEmail.trim(),
                            "childName"    to pendingName,
                            "childSchool"  to pendingChildSchool,
                            "childGrade"   to pendingChildGrade,
                            "childClass"   to pendingChildClass,
                            "childNumber"  to pendingChildNumber,
                            "birthDate"    to pendingBirthDate,
                            "classCode"    to classCode,
                            "approved"     to true
                        )
                    ).await()
                } catch (_: Exception) {}
            }
            auth.signOut()
            _authState.value = AuthState.Success
        } catch (e: FirebaseAuthException) {
            _authState.value = AuthState.Error(mapAuthError(e.errorCode))
        } catch (e: Exception) {
            _authState.value = AuthState.Error(mapGenericError(e))
        }
    }

    // ─── 공통 ────────────────────────────────────────────────────────
    private suspend fun loadUserData(uid: String) {
        try {
            val doc = db.collection("users").document(uid).get().await()
            _userData.value = UserData(
                email        = doc.getString("email")        ?: "",
                childName    = doc.getString("childName")    ?: "",
                childSchool  = doc.getString("childSchool")  ?: "",
                childGrade   = (doc.getLong("childGrade")    ?: 0L).toInt(),
                childClass   = (doc.getLong("childClass")    ?: 0L).toInt(),
                childNumber  = (doc.getLong("childNumber")   ?: 0L).toInt(),
                role         = doc.getString("role")         ?: "",
                birthDate    = doc.getString("birthDate")    ?: ""
            )
        } catch (_: Exception) {}
    }

    fun clearUserData() {
        _userData.value = null
        if (auth.currentUser != null) auth.signOut()
    }

    fun resetState() { _authState.value = AuthState.Idle }

    private fun mapAuthError(code: String): String = when (code) {
        "ERROR_INVALID_EMAIL"           -> "올바른 이메일 형식이 아닙니다."
        "ERROR_WRONG_PASSWORD",
        "ERROR_INVALID_CREDENTIAL"      -> "이메일 또는 비밀번호가 올바르지 않습니다."
        "ERROR_EMAIL_ALREADY_IN_USE"    -> "이미 사용 중인 이메일입니다."
        "ERROR_WEAK_PASSWORD"           -> "비밀번호는 6자 이상이어야 합니다."
        "ERROR_USER_NOT_FOUND"          -> "등록되지 않은 계정입니다."
        "ERROR_USER_DISABLED"           -> "비활성화된 계정입니다."
        "ERROR_TOO_MANY_REQUESTS"       -> "잠시 후 다시 시도해주세요."
        else                            -> "오류가 발생했습니다. ($code)"
    }

    private fun mapGenericError(e: Exception): String = when {
        e.message?.contains("network", ignoreCase = true) == true -> "네트워크 연결을 확인해주세요."
        else -> "오류가 발생했습니다. 다시 시도해주세요."
    }
}
