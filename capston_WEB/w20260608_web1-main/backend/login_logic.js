import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-app.js";
import { getAuth, signInWithEmailAndPassword, sendPasswordResetEmail, createUserWithEmailAndPassword, signOut } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-auth.js";
import { getFirestore, doc, getDoc, setDoc } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-firestore.js";

const firebaseConfig = {
    apiKey: "AIzaSyARu5lhGNWTF20f42YLquQQZeRM9r3H0Cw",
    authDomain: "edutrack-c5053.firebaseapp.com",
    projectId: "edutrack-c5053",
    storageBucket: "edutrack-c5053.firebasestorage.app",
    messagingSenderId: "493529902405",
    appId: "1:493529902405:web:87b24e77459af6a62da22c",
    measurementId: "G-P6TDZV49CF"
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

// [1] 섹션 전환
window.toggleSection = (type) => {
    const sections = ['login-section', 'signup-section', 'forgot-pw-section'];
    sections.forEach(sec => {
        document.getElementById(sec).style.display = (sec === type + '-section') ? 'block' : 'none';
    });
};

// [2] 로그인 로직
const performLogin = async () => {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;
    if(!email || !password) return alert("이메일과 비밀번호를 모두 입력해주세요.");

    const btn = document.getElementById("loginBtn");
    btn.disabled = true;
    btn.textContent = "로그인 중...";

    try {
        const userCredential = await signInWithEmailAndPassword(auth, email, password);
        const user = userCredential.user;
        const userDocSnap = await getDoc(doc(db, "users", user.uid));

        if (userDocSnap.exists()) {
            const userData = userDocSnap.data();
            if (userData.role === 'admin') {
                alert("관리자 계정으로 접속합니다.");
                window.location.href = "/frontend/admin.html";
            } else if (userData.role === 'teacher') {
                if (userData.status === 'approved') {
                    alert("선생님 환영합니다!");
                    window.location.href = "/frontend/dashboard.html";
                } else {
                    alert("가입 승인 대기 중입니다. 관리자 승인 후 로그인 가능합니다.");
                    await signOut(auth);
                }
            }
        } else {
            alert("권한 정보가 없습니다. 관리자에게 문의하세요.");
            await signOut(auth);
        }
    } catch (error) {
        alert("로그인 실패: 정보를 다시 확인해주세요.");
    } finally {
        btn.disabled = false;
        btn.textContent = "로그인";
    }
};

document.getElementById("loginBtn").addEventListener("click", performLogin);

// Enter 키로 로그인
['email', 'password'].forEach(id => {
    document.getElementById(id).addEventListener('keydown', e => {
        if (e.key === 'Enter') performLogin();
    });
});

// Enter 키로 비밀번호 재설정 요청
document.getElementById('reset-email').addEventListener('keydown', e => {
    if (e.key === 'Enter') document.getElementById('resetPwBtn').click();
});

// ==========================================
// 🚀 [3] 비밀번호 재설정 (비밀번호 찾기) 로직 추가됨
// ==========================================
document.getElementById("resetPwBtn").addEventListener("click", async () => {
    const email = document.getElementById("reset-email").value;

    if (!email || !email.includes("@")) {
        return alert("비밀번호를 재설정할 이메일 주소를 정확히 입력해주세요.");
    }

    const btn = document.getElementById("resetPwBtn");
    btn.disabled = true;
    btn.textContent = "발송 중...";

    try {
        // 파이어베이스 자체 서버에서 재설정 링크 발송
        await sendPasswordResetEmail(auth, email);

        alert(`📧 [${email}] 주소로 비밀번호 재설정 링크가 발송되었습니다!\n메일함을 확인해주세요. (스팸함도 꼭 확인하세요!)`);

        // 발송 후 다시 로그인 화면으로 이동
        toggleSection('login');
    } catch (error) {
        console.error("비밀번호 재설정 에러:", error);
        if (error.code === 'auth/user-not-found') {
            alert("해당 이메일로 가입된 계정이 없습니다.");
        } else {
            alert("메일 발송 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    } finally {
        btn.disabled = false;
        btn.textContent = "재설정 링크 발송";
    }
});

// [4] 인증번호 발송 (가입용)
let generatedAuthCode = ""; 
document.getElementById("sendAuthBtn").addEventListener("click", () => {
    const email = document.getElementById("signup-email").value;
    if(!email || !email.includes("@")) return alert("유효한 이메일을 먼저 입력해주세요.");

    generatedAuthCode = Math.floor(100000 + Math.random() * 900000).toString();
    const templateParams = { to_email: email, auth_code: generatedAuthCode };
    const sendBtn = document.getElementById("sendAuthBtn");
    
    sendBtn.innerText = "발송 중...";
    sendBtn.disabled = true;

    emailjs.send("service_aqa4yx4", "template_ryod9as", templateParams)
        .then(() => alert(`📧 ${email} 계정으로 인증번호가 발송되었습니다!`))
        .catch(() => alert("메일 발송 실패."))
        .finally(() => { sendBtn.innerText = "인증번호 보내기"; sendBtn.disabled = false; });
});

// [5] 회원가입 (이미지를 텍스트로 변환하여 DB 저장)
document.getElementById("signupBtn").addEventListener("click", async () => {
    const name = document.getElementById("signup-name").value.trim();
    const email = document.getElementById("signup-email").value;
    const pw = document.getElementById("signup-pw").value;
    const pwConfirm = document.getElementById("signup-pw-confirm").value;
    const authCodeInput = document.getElementById("auth-code").value;
    const fileInput = document.getElementById("teacher-auth-file");
    const authFile = fileInput.files[0];

    if(!name) return alert("이름(실명)을 입력해주세요.");
    if(!email || !pw || !pwConfirm || !authFile) return alert("증빙 서류를 포함한 모든 정보를 입력해주세요.");
    if(pw !== pwConfirm) return alert("비밀번호가 다릅니다.");
    if(pw.length < 6) return alert("비밀번호는 6자 이상이어야 합니다.");
    if(!authCodeInput || authCodeInput !== generatedAuthCode) return alert("인증번호가 틀립니다.");
    if(authFile.size > 2 * 1024 * 1024) return alert("파일 크기는 2MB 이하여야 합니다.");
    const allowedTypes = ['image/jpeg', 'image/png', 'application/pdf'];
    if(!allowedTypes.includes(authFile.type)) return alert("JPG, PNG, PDF 파일만 첨부 가능합니다.");

    const signupBtn = document.getElementById("signupBtn");
    signupBtn.disabled = true;
    signupBtn.textContent = "처리 중...";

    // 파일을 텍스트(Base64)로 변환
    const reader = new FileReader();
    reader.readAsDataURL(authFile);
    reader.onload = async () => {
        const base64String = reader.result;

        try {
            const userCredential = await createUserWithEmailAndPassword(auth, email, pw);
            const user = userCredential.user;

            await setDoc(doc(db, "users", user.uid), {
                uid: user.uid,
                name: name,
                email: email,
                role: "teacher",
                status: "pending",
                authFileData: base64String,
                createdAt: new Date().toISOString()
            });

            alert("회원가입 완료! 관리자 승인 후 이용 가능합니다.");
            toggleSection('login');
            await signOut(auth);
        } catch (error) {
            alert("가입 에러: " + error.message);
        } finally {
            signupBtn.disabled = false;
            signupBtn.textContent = "회원가입 완료";
        }
    };
    reader.onerror = () => {
        alert("파일을 읽는 중 오류가 발생했습니다.");
        signupBtn.disabled = false;
        signupBtn.textContent = "회원가입 완료";
    };
});