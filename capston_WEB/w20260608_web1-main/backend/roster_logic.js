import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-app.js";
import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-auth.js";
import { getFirestore, collection, onSnapshot, query, where } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-firestore.js";

const legacyConfig = {
    apiKey: "AIzaSyARu5lhGNWTF20f42YLquQQZeRM9r3H0Cw",
    authDomain: "edutrack-c5053.firebaseapp.com",
    projectId: "edutrack-c5053"
};
const newConfig = {
    apiKey: "AIzaSyDzi7vvF4A5E_Mg9SMJrZZ8Z7RMCxgLcc4",
    authDomain: "edutrack-bfd57.firebaseapp.com",
    projectId: "edutrack-bfd57",
    storageBucket: "edutrack-bfd57.firebasestorage.app"
};

const app          = initializeApp(legacyConfig);
const auth         = getAuth(app);
const secondaryApp = initializeApp(newConfig, "rosterApp");
const db           = getFirestore(secondaryApp);

// URL 파라미터로 초기 학년 설정
const params = new URLSearchParams(window.location.search);
let currentGrade = parseInt(params.get('grade')) || 1;
let currentClass = null;
let allStudents   = [];

// ── 학년 선택 ──
window.selectGrade = (grade) => {
    currentGrade = grade;
    currentClass = null;
    renderGradeTabs();
    renderClassTabs();
    renderStudents();
};

// ── 반 선택 ──
window.selectClass = (cls) => {
    currentClass = cls;
    renderClassTabs();
    renderStudents();
};

// ── 학년 탭 렌더 ──
const renderGradeTabs = () => {
    [1, 2, 3].forEach(g => {
        document.getElementById(`gtab-${g}`)?.classList.toggle('active', g === currentGrade);
    });
};

// ── 해당 학년의 반 목록 (Firebase 데이터 기준 동적 생성) ──
const getClasses = (grade) => {
    const set = new Set();
    allStudents
        .filter(s => String(s.grade) === String(grade))
        .forEach(s => { if (s.class) set.add(String(s.class)); });
    return Array.from(set).sort((a, b) => parseInt(a) - parseInt(b));
};

// ── 반 탭 렌더 ──
const renderClassTabs = () => {
    const bar     = document.getElementById('classTabs');
    const classes = getClasses(currentGrade);

    if (!classes.length) {
        bar.innerHTML = '<span style="color:#a0aec0; font-size:13px;">해당 학년 응시자 없음</span>';
        currentClass = null;
        return;
    }

    // currentClass 가 목록에 없으면 첫 번째로 초기화
    if (!classes.includes(String(currentClass))) currentClass = classes[0];

    bar.innerHTML = classes.map(c => `
        <button class="class-tab ${String(c) === String(currentClass) ? 'active' : ''}"
                onclick="selectClass('${c}')">${c}반</button>
    `).join('');
};

// ── 학생 목록 렌더 ──
const renderStudents = () => {
    const title = document.getElementById('rosterTitle');
    const tbody = document.getElementById('rosterBody');

    if (!currentClass) {
        title.innerHTML = `${currentGrade}학년`;
        tbody.innerHTML = '<tr class="empty-row"><td colspan="4">반을 선택해주세요.</td></tr>';
        return;
    }

    const students = allStudents
        .filter(s => String(s.grade) === String(currentGrade) && String(s.class) === String(currentClass))
        .sort((a, b) => (a.name || '').localeCompare(b.name || '', 'ko'));

    title.innerHTML = `${currentGrade}학년 ${currentClass}반
        <span class="roster-count">총 ${students.length}명</span>`;

    if (!students.length) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="4">해당 반에 응시자가 없습니다.</td></tr>';
        return;
    }

    tbody.innerHTML = students.map((s, i) => {
        let scoreDisplay;
        if (s.score === '부정행위') {
            scoreDisplay = `<span class="score-badge" style="background:#c0392b;">🚨 부정행위</span>`;
        } else {
            const bg = s.score >= 80 ? '#2ecc71' : '#f39c12';
            scoreDisplay = `<span class="score-badge" style="background:${bg};">${s.score}점</span>`;
        }

        const keywords = s.keywords
            ? s.keywords.map(k =>
                `<span style="background:#e8f0fe; color:#1a73e8; padding:2px 8px; border-radius:12px; font-size:11px; margin-right:4px;">${k}</span>`
              ).join('')
            : '<span style="color:#a0aec0;">-</span>';

        return `
            <tr>
                <td style="color:#718096;">${i + 1}</td>
                <td><strong>${s.name || '-'}</strong></td>
                <td>${scoreDisplay}</td>
                <td>${keywords}</td>
            </tr>`;
    }).join('');
};

// ── 인증 확인 후 실시간 데이터 구독 ──
onAuthStateChanged(auth, (user) => {
    if (!user) { location.href = '/frontend/login.html'; return; }

    onSnapshot(query(collection(db, 'student_scores'), where('teacherId', '==', user.uid)), (snapshot) => {
        allStudents = [];
        snapshot.forEach(doc => allStudents.push(doc.data()));

        renderGradeTabs();
        renderClassTabs();
        renderStudents();
    });
});
