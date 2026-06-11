import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-app.js";
import { getAuth, signOut } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-auth.js";
import { getFirestore, collection, query, where, onSnapshot, doc, updateDoc, deleteDoc } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-firestore.js";

const firebaseConfig = {
    apiKey: "AIzaSyARu5lhGNWTF20f42YLquQQZeRM9r3H0Cw",
    authDomain: "edutrack-c5053.firebaseapp.com",
    projectId: "edutrack-c5053",
    storageBucket: "edutrack-c5053.firebasestorage.app",
    messagingSenderId: "493529902405",
    appId: "1:493529902405:web:87b24e77459af6a62da22c"
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

document.getElementById("logoutBtn").onclick = () => signOut(auth).then(() => location.href = "/frontend/login.html");

// [1] 화면 전환
window.showSection = (sectionId) => {
    document.querySelectorAll('.content-section').forEach(sec => sec.classList.remove('active'));
    document.getElementById(sectionId).classList.add('active');
    document.querySelectorAll('.topbar-shortcut-btn').forEach(btn => btn.classList.remove('active'));
    document.getElementById('nav-' + sectionId).classList.add('active');
};

// [2] 🚀 실시간 대기열 불러오기 (수정됨)
const q = query(collection(db, "users"), where("role", "==", "teacher"), where("status", "==", "pending"));

onSnapshot(q, (snapshot) => {
    const list = document.getElementById('approval-list');
    const pendingCount = document.getElementById('pending-count');
    let html = "";
    
    snapshot.forEach((docSnap) => {
        const t = docSnap.data();
        
        // ⭐️ 핵심 수정: DB 내부에 uid 필드가 없어도, 문서 자체의 이름(ID)을 무조건 가져오도록 변경
        const userId = docSnap.id; 
        
        html += `
            <tr id="teacher-${userId}">
                <td>${t.createdAt ? new Date(t.createdAt).toLocaleDateString('ko-KR') : '-'}</td>
                <td><strong>${t.email}</strong></td>
                <td><span style="color:orange; font-weight:bold;">대기중</span></td>
                <td>
                    <button onclick="viewDoc('${userId}')" style="cursor:pointer; color:#2196F3; border:none; background:none; text-decoration:underline; font-weight:bold;">📄 서류 확인</button>
                </td>
                <td>
                    <button class="btn btn-approve" onclick="approveTeacher('${userId}', '${t.email}')">승인 완료</button>
                    <button class="btn btn-reject" onclick="rejectTeacher('${userId}')">가입 거절</button>
                </td>
            </tr>`;
        
        // 버튼 클릭 시 데이터를 찾기 위해 임시로 윈도우 객체에 데이터 저장 (긴 문자열 처리용)
        if (!window.tempAuthData) window.tempAuthData = {};
        window.tempAuthData[userId] = t.authFileData;
    });
    
    list.innerHTML = html || '<tr><td colspan="5" style="text-align:center; padding:30px; color:#999;">대기 중인 승인 요청이 없습니다.</td></tr>';
    pendingCount.innerText = snapshot.size;
    document.getElementById('pending-stat').innerText = snapshot.size + '명';
});

// [3] 서류 확인용 새 창 띄우기
window.viewDoc = (uid) => {
    const data = window.tempAuthData?.[uid];
    if (!data) return alert("이미지 데이터를 찾을 수 없습니다.");
    const newWindow = window.open('', '_blank');
    if (!newWindow) return alert("팝업이 차단되었습니다. 브라우저 팝업 허용 후 다시 시도해주세요.");
    newWindow.document.write(`<title>증빙 서류 확인</title><img src="${data}" style="max-width:100%;">`);
};

// [4] 가입 승인
window.approveTeacher = async (uid, email) => {
    if(confirm(`${email} 선생님의 가입을 승인하시겠습니까?`)) {
        try {
            await updateDoc(doc(db, "users", uid), { status: "approved" });
            emailjs.send("service_aqa4yx4", "template_approval", {
                to_email: email,
                message: "에듀트랙 가입 승인이 완료되었습니다!"
            });
            alert("승인이 완료되었습니다.");
        } catch (e) { alert("승인 실패: " + e.message); }
    }
};

// [5] 가입 거절
window.rejectTeacher = async (uid) => {
    if(confirm("가입 요청을 거절하시겠습니까?")) {
        try {
            await deleteDoc(doc(db, "users", uid));
            alert("거절 처리되었습니다.");
        } catch(e) { alert("거절 실패: " + e.message); }
    }
};