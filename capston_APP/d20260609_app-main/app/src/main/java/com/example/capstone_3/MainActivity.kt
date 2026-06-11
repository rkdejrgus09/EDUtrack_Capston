package com.example.capstone_3

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.iamport.sdk.domain.core.Iamport
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.example.capstone_3.auth.AuthState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.capstone_3.auth.AuthViewModel
import com.example.capstone_3.common.Blue
import com.example.capstone_3.common.LightGray
import com.example.capstone_3.common.LocalThemeColor
import com.example.capstone_3.common.Screen
import com.example.capstone_3.screen.auth.*
import com.example.capstone_3.screen.main.MainScreen
import com.example.capstone_3.ui.theme.Capstone_3Theme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Capstone_3Theme {
                AppNavigation()
            }
        }
        Iamport.init(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Iamport.close()
    }
}

@Composable
fun AppNavigation() {
    val authViewModel: AuthViewModel = viewModel()
    val userData  by authViewModel.userData.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    var screen by remember { mutableStateOf(Screen.Login) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success && screen == Screen.Login) {
            screen = Screen.Main
        }
    }

    var childGrade   by remember { mutableStateOf(1) }
    var childClass   by remember { mutableStateOf(1) }
    var childName    by remember { mutableStateOf("") }
    var themeColor   by remember { mutableStateOf(Blue) }

    // 회원가입 플로우 상태 (SignUpFlowScreen이 재구성돼도 유지됨)
    var signUpStep            by remember { mutableIntStateOf(0) }
    var signUpEmail           by remember { mutableStateOf("") }
    var signUpPassword        by remember { mutableStateOf("") }
    var signUpPasswordConfirm by remember { mutableStateOf("") }
    var signUpBirthDate       by remember { mutableStateOf("") }
    var signUpNumber          by remember { mutableStateOf("") }

    fun resetSignUpForm() {
        signUpStep = 0
        signUpEmail = ""; signUpPassword = ""; signUpPasswordConfirm = ""
        signUpBirthDate = ""; signUpNumber = ""
        childGrade = 1; childClass = 1; childName = ""
    }

    // SignUp 화면 내부 뒤로가기는 SignUpFlowScreen의 BackHandler가 처리
    BackHandler(enabled = screen != Screen.Login && screen != Screen.Main && screen != Screen.SignUp) {
        when (screen) {
            Screen.ChildInfoSchool -> screen = Screen.SignUp
            Screen.ForgotPassword  -> screen = Screen.Login
            else                   -> {}
        }
    }

    CompositionLocalProvider(LocalThemeColor provides themeColor) {
        Surface(modifier = Modifier.fillMaxSize(), color = LightGray) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    val forward = targetState.ordinal >= initialState.ordinal
                    if (forward) {
                        (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 3 } +
                            fadeIn(tween(300, easing = FastOutSlowInEasing))) togetherWith
                        (slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 3 } +
                            fadeOut(tween(150)))
                    } else {
                        (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 3 } +
                            fadeIn(tween(300, easing = FastOutSlowInEasing))) togetherWith
                        (slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 3 } +
                            fadeOut(tween(150)))
                    }
                },
                label = "nav"
            ) { currentScreen ->
            when (currentScreen) {
                Screen.Login -> LoginScreen(
                    viewModel          = authViewModel,
                    onNavigateToSignUp = { screen = Screen.SignUp },
                    onLoginSuccess     = { screen = Screen.Main },
                    onForgotPassword   = { screen = Screen.ForgotPassword }
                )
                Screen.ForgotPassword -> ForgotPasswordScreen(
                    onBackToLogin = { screen = Screen.Login }
                )
                Screen.SignUp -> SignUpFlowScreen(
                    step                    = signUpStep,
                    onStepChange            = { signUpStep = it },
                    email                   = signUpEmail,
                    onEmailChange           = { signUpEmail = it },
                    password                = signUpPassword,
                    onPasswordChange        = { signUpPassword = it },
                    passwordConfirm         = signUpPasswordConfirm,
                    onPasswordConfirmChange = { signUpPasswordConfirm = it },
                    name                    = childName,
                    onNameChange            = { childName = it },
                    birthDate               = signUpBirthDate,
                    onBirthDateChange       = { signUpBirthDate = it },
                    onComplete              = { screen = Screen.ChildInfoSchool },
                    onBack                  = { resetSignUpForm(); screen = Screen.Login }
                )
                Screen.ChildInfoSchool -> SchoolGradeCodeScreen(
                    viewModel    = authViewModel,
                    email        = signUpEmail,
                    password     = signUpPassword,
                    birthDate    = signUpBirthDate,
                    childName    = childName,
                    initialGrade = childGrade,
                    initialClass = childClass,
                    onComplete   = { resetSignUpForm(); screen = Screen.Login },
                    onBack       = { screen = Screen.SignUp }
                )
                Screen.Main  -> MainScreen(
                    userData      = userData,
                    onLogout      = { authViewModel.clearUserData(); screen = Screen.Login },
                    onThemeChange = { themeColor = it }
                )
            }
            } // AnimatedContent
        }
    }
}
