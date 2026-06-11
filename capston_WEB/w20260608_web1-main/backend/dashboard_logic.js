import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-app.js";
import { getAuth, signOut, onAuthStateChanged, sendPasswordResetEmail } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-auth.js";
import { getFirestore, collection, addDoc, onSnapshot, query, orderBy, limit, doc, getDoc, setDoc, getDocs, where, deleteDoc, updateDoc } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-firestore.js";
import { getStorage, ref as sRef, uploadBytes, getDownloadURL } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-storage.js";

// [기존 설정] 인증 전용
const legacyConfig = { 
    apiKey: "AIzaSyARu5lhGNWTF20f42YLquQQZeRM9r3H0Cw", 
    authDomain: "edutrack-c5053.firebaseapp.com", 
    projectId: "edutrack-c5053"
};
const app          = initializeApp(legacyConfig);
const auth         = getAuth(app);
const legacyFs     = getFirestore(app);   

// [앱 설정] 안드로이드 앱의 Firebase 프로젝트
const appConfig = {
    apiKey:    "AIzaSyCLl30ML3_qOVrNkVcW3kliij_xMaI3QDE",
    authDomain: "capstone2-b1cb3.firebaseapp.com",
    projectId: "capstone2-b1cb3"
};
const mobileApp = initializeApp(appConfig, "mobileApp");
const legacyDb  = getFirestore(mobileApp); 

// [신규 설정] 데이터 전용 (bfd57)
const newConfig = {
    apiKey: "AIzaSyDzi7vvF4A5E_Mg9SMJrZZ8Z7RMCxgLcc4", 
    authDomain: "edutrack-bfd57.firebaseapp.com", 
    projectId: "edutrack-bfd57",
    storageBucket: "edutrack-bfd57.firebasestorage.app"
};
const secondaryApp = initializeApp(newConfig, "storageApp");
const db = getFirestore(secondaryApp);     
const storage = getStorage(secondaryApp); 

let currentFilesToUpload = []; // 다중 이미지 또는 PDF 1개
window.classStatsData = [];
window.testKeywordsMap = {};
window.fixedScheduleData = []; // 고정 일정 공유 데이터 (홈 탭 다가오는 일정과 연동)

const TOPBAR_SHORTCUTS = `
    <button class="topbar-shortcut-btn" id="tbNoticeBtn" onclick="showView('noticeView')">공지사항</button>
    <button class="topbar-shortcut-btn" id="tbUpdateBtn" onclick="showView('updateView')">최근 업데이트</button>
`;

document.addEventListener('DOMContentLoaded', () => {
    const actions = document.getElementById('topBarActions');
    if (actions) actions.innerHTML = TOPBAR_SHORTCUTS;
});

const HOME_GROUP   = ['homeView', 'noticeView', 'updateView'];

window.showView = (viewId) => {
    document.querySelectorAll('.content-section').forEach(s => s.classList.remove('active'));
    document.getElementById(viewId)?.classList.add('active');
    document.querySelectorAll('.nav-item').forEach(a => a.classList.remove('active'));
    const navItem = document.getElementById('nav-' + viewId.replace('View', ''));
    if (navItem) navItem.classList.add('active');

    // reportView로 전환 시 항상 시험 목록 화면으로 리셋
    if (viewId === 'reportView') window.showExamList?.();
    // homeView로 전환 시 다가오는 일정 즉시 갱신
    if (viewId === 'homeView') { renderNextClass(); renderUpcomingSchedule(); }

    const topBar = document.querySelector('.top-bar');
    const actions = document.getElementById('topBarActions');

    if (HOME_GROUP.includes(viewId)) {
        if (topBar) topBar.style.display = '';
        if (actions) {
            actions.innerHTML = TOPBAR_SHORTCUTS;
            document.getElementById('tbNoticeBtn')?.classList.toggle('active', viewId === 'noticeView');
            document.getElementById('tbUpdateBtn')?.classList.toggle('active', viewId === 'updateView');
        }
    } else {
        if (topBar) topBar.style.display = 'none';
    }

    // nav 인디케이터: 보조 바 전용 뷰에서는 숨김
    const indicator = document.getElementById('navIndicator');
    const hideIndicator = ['noticeView', 'updateView'];
    if (indicator && hideIndicator.includes(viewId)) {
        indicator.style.width = '0px';
    }

    if (viewId === 'noticeView') renderNoticePage();
    if (viewId === 'updateView') renderUpdatePage();
    if (viewId === 'wrongAnswerView') renderWrongAnswerView();

    window.updateNavIndicator?.();
};

window.renderWrongAnswerView = () => {
    const placeholder = document.getElementById('analysisPlaceholder');
    const content = document.getElementById('analysisContent');
    if (!currentAnalysisExamId) {
        if (placeholder) placeholder.style.display = 'flex';
        if (content) content.style.display = 'none';
        return;
    }
    if (placeholder) placeholder.style.display = 'none';
    if (content) content.style.display = '';

    const all = window.classStatsData || [];
    const totalStudents = all.length;
    const lowScoreStudents = all.filter(s => typeof s.score === 'number' && s.score < 80);
    const clinicStudents = all.filter(s => typeof s.score === 'number' && s.score <= 70);
    const flaggedStudents = all.filter(s => s.score === '부정행위');

    const weakMap = {};
    all.forEach(s => {
        const keys = Array.isArray(s.keywords) ? s.keywords : s.subject ? [s.subject] : [];
        keys.forEach(k => {
            if (!k) return;
            weakMap[k] = (weakMap[k] || 0) + 1;
        });
    });
    const topWeak = Object.entries(weakMap)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 3)
        .map(([k, c]) => `${k} (${c})`)
        .join(', ') || '데이터 없음';

    const summaryGrid = document.getElementById('noteSummaryGrid');
    if (summaryGrid) {
        summaryGrid.innerHTML = `
            <div style="padding:16px; background:#f8fafc; border-radius:12px;">
                <div style="font-size:13px; color:#718096; margin-bottom:8px;">전체 학생</div>
                <div style="font-size:28px; font-weight:700; color:#1f2937;">${totalStudents}</div>
            </div>
            <div style="padding:16px; background:#f8fafc; border-radius:12px;">
                <div style="font-size:13px; color:#718096; margin-bottom:8px;">주의 요망 노트</div>
                <div style="font-size:28px; font-weight:700; color:#b45309;">${lowScoreStudents.length}</div>
            </div>
            <div style="padding:16px; background:#f8fafc; border-radius:12px;">
                <div style="font-size:13px; color:#718096; margin-bottom:8px;">AI 클리닉 대상</div>
                <div style="font-size:28px; font-weight:700; color:#c026d3;">${clinicStudents.length}</div>
            </div>
            <div style="padding:16px; background:#f8fafc; border-radius:12px;">
                <div style="font-size:13px; color:#718096; margin-bottom:8px;">자주 등장한 취약 키워드</div>
                <div style="font-size:14px; color:#1f2937; line-height:1.5;">${topWeak}</div>
            </div>
        `;
    }

    // 반별 평균 비교 차트
    const classAvgCanvas = document.getElementById('classAvgCanvas');
    const classAvgEmpty  = document.getElementById('classAvgEmpty');
    if (classAvgCanvas) {
        if (window._classAvgChart) { window._classAvgChart.destroy(); window._classAvgChart = null; }

        const numericAll = all.filter(s => typeof s.score === 'number' && s.class != null);
        const classMap = {};
        numericAll.forEach(s => {
            const key = String(s.class);
            if (!classMap[key]) classMap[key] = [];
            classMap[key].push(s.score);
        });
        const sortedClasses = Object.keys(classMap).sort((a, b) => parseInt(a) - parseInt(b));

        if (!sortedClasses.length) {
            classAvgCanvas.style.display = 'none';
            if (classAvgEmpty) classAvgEmpty.style.display = '';
        } else {
            classAvgCanvas.style.display = '';
            if (classAvgEmpty) classAvgEmpty.style.display = 'none';

            const avgs = sortedClasses.map(c => {
                const scores = classMap[c];
                return Math.round(scores.reduce((a, b) => a + b, 0) / scores.length);
            });
            const primaryColor = getComputedStyle(document.documentElement).getPropertyValue('--primary').trim() || '#3182F6';

            window._classAvgChart = new Chart(classAvgCanvas, {
                type: 'bar',
                data: {
                    labels: sortedClasses.map(c => `${c}반`),
                    datasets: [{
                        label: '평균 점수',
                        data: avgs,
                        backgroundColor: primaryColor + 'cc',
                        borderColor: primaryColor,
                        borderWidth: 1,
                        borderRadius: 6,
                        borderSkipped: false,
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        legend: { display: false },
                        tooltip: { callbacks: { label: ctx => ` ${ctx.parsed.y}점` } }
                    },
                    scales: {
                        y: { beginAtZero: false, min: 0, max: 100, ticks: { stepSize: 20 } }
                    }
                }
            });
        }
    }

    const wrongSummary = document.getElementById('wrongSummary');
    if (wrongSummary) {
        wrongSummary.innerHTML = `
            <div style="padding:16px; background:#eef2ff; border-radius:12px;">
                <div style="font-size:13px; color:#4f46e5; margin-bottom:8px;">클래스 전체 상태</div>
                <div style="font-size:18px; font-weight:700; color:#1e293b;">${totalStudents ? `${Math.round((totalStudents - lowScoreStudents.length) / totalStudents * 100)}%` : '—'}</div>
                <div style="font-size:13px; color:#64748b; margin-top:6px;">80점 이상 학생 비율</div>
            </div>
            <div style="padding:16px; background:#fef3c7; border-radius:12px;">
                <div style="font-size:13px; color:#b45309; margin-bottom:8px;">즉시 대응 필요</div>
                <div style="font-size:18px; font-weight:700; color:#92400e;">${clinicStudents.length}명</div>
                <div style="font-size:13px; color:#7c2d12; margin-top:6px;">70점 이하 학생 수</div>
            </div>
            <div style="padding:16px; background:#f8fafc; border-radius:12px;">
                <div style="font-size:13px; color:#475569; margin-bottom:8px;">부정행위 기록</div>
                <div style="font-size:18px; font-weight:700; color:#0f172a;">${flaggedStudents.length}건</div>
                <div style="font-size:13px; color:#64748b; margin-top:6px;">알림 / 재분석 대상</div>
            </div>
        `;
    }

    // 점수 분포 차트
    const canvas = document.getElementById('scoreDistCanvas');
    const emptyEl = document.getElementById('scoreDistEmpty');
    const numericStudents = all.filter(s => typeof s.score === 'number');

    if (canvas) {
        if (window._scoreDistChart) { window._scoreDistChart.destroy(); window._scoreDistChart = null; }

        if (!numericStudents.length) {
            canvas.style.display = 'none';
            if (emptyEl) emptyEl.style.display = '';
        } else {
            canvas.style.display = '';
            if (emptyEl) emptyEl.style.display = 'none';

            const ranges = [
                { label: '0~59점',   min: 0,  max: 59,  color: '#ef4444' },
                { label: '60~69점',  min: 60, max: 69,  color: '#f97316' },
                { label: '70~79점',  min: 70, max: 79,  color: '#f59e0b' },
                { label: '80~89점',  min: 80, max: 89,  color: '#84cc16' },
                { label: '90~100점', min: 90, max: 100, color: '#22c55e' },
            ];
            const counts = ranges.map(r => numericStudents.filter(s => s.score >= r.min && s.score <= r.max).length);

            window._scoreDistChart = new Chart(canvas, {
                type: 'bar',
                data: {
                    labels: ranges.map(r => r.label),
                    datasets: [{
                        data: counts,
                        backgroundColor: ranges.map(r => r.color),
                        borderRadius: 6,
                        borderSkipped: false,
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: { label: ctx => ` ${ctx.parsed.y}명` }
                        }
                    },
                    scales: {
                        y: { beginAtZero: true, ticks: { stepSize: 1, precision: 0 } }
                    }
                }
            });
        }
    }
};

// =====================================================================
// 📊 학습 분석 탭 - 시험 선택 & 데이터 로드
// =====================================================================
let analysisExamsList = [];
let currentAnalysisExamId = null;

const populateAnalysisDropdown = (tests) => {
    const sel = document.getElementById('analysisExamSelect');
    if (!sel) return;
    const current = sel.value;
    sel.innerHTML = '<option value="">-- 분석할 시험을 선택하세요 --</option>';
    tests.forEach(t => {
        const dateStr = t.createdAt ? new Date(t.createdAt).toLocaleDateString('ko-KR') : '';
        const opt = document.createElement('option');
        opt.value = t.id;
        opt.textContent = `${t.subject} (${t.grade}학년 · ${dateStr})`;
        sel.appendChild(opt);
    });
    if (current && [...sel.options].some(o => o.value === current)) sel.value = current;
};

const setStudentListBtnVisible = (visible) => {
    const btn = document.getElementById('toStudentListBtn');
    if (btn) btn.style.display = visible ? '' : 'none';
};

window.onAnalysisExamChange = async (examId) => {
    currentAnalysisExamId = examId || null;
    setStudentListBtnVisible(!!examId);
    if (!examId) {
        window.classStatsData = [];
        renderWrongAnswerView();
        return;
    }
    const exam = analysisExamsList.find(t => t.id === examId);
    if (!exam) return;
    await loadAnalysisData(examId, exam.subject, exam.grade);
};

const loadAnalysisData = async (examId, subject, grade) => {
    window.classStatsData = [];
    try {
        const snap = await getDocs(query(
            collection(db, 'student_scores'),
            where('testId', '==', examId)
        ));
        snap.forEach(docSnap => {
            window.classStatsData.push(docSnap.data());
        });
        // testId 없이 subject+grade로 저장된 구 데이터도 포함
        if (!window.classStatsData.length) {
            const snap2 = await getDocs(query(
                collection(db, 'student_scores'),
                where('subject', '==', subject),
                where('grade', '==', grade)
            ));
            snap2.forEach(docSnap => {
                const data = docSnap.data();
                if (!data.testId) window.classStatsData.push(data);
            });
        }
    } catch (e) {
        console.error('학습 분석 데이터 로드 실패:', e);
    }
    renderWrongAnswerView();
};

window.openAnalysisForExam = async (examId) => {
    showView('wrongAnswerView');
    const sel = document.getElementById('analysisExamSelect');
    if (sel) sel.value = examId;
    currentAnalysisExamId = examId;
    setStudentListBtnVisible(true);
    const exam = analysisExamsList.find(t => t.id === examId);
    if (exam) await loadAnalysisData(examId, exam.subject, exam.grade);
};

window.goToStudentList = () => {
    if (!currentAnalysisExamId) return;
    const exam = analysisExamsList.find(t => t.id === currentAnalysisExamId);
    if (!exam) return;
    showView('reportView');
    selectExam(currentAnalysisExamId, exam.subject, exam.grade, '', exam.createdAt || '');
};

// ── 공지사항 페이지 렌더 ──
const renderNoticePage = () => {
    const list = document.getElementById('noticePageList');
    if (!list) return;
    closeNuDetail('notice');   // 재진입 시 우측 패널 초기화
    list.innerHTML = ALL_NOTICES.map((n, i) => `
        <div class="notice-full-item" onclick="togglePageItem('notice', ${i}, this)">
            <div class="notice-full-header">
                <span class="notice-text">${n.isNew ? `<span class="badge-n" id="npbadge-${i}">N</span> ` : ''}${n.title}</span>
                <span class="notice-date">${n.date}</span>
            </div>
        </div>`).join('');
};

// ── 최근 업데이트 페이지 렌더 ──
const renderUpdatePage = () => {
    const list = document.getElementById('updatePageList');
    if (!list) return;
    closeNuDetail('update');   // 재진입 시 우측 패널 초기화
    list.innerHTML = ALL_UPDATES.map((u, i) => `
        <div class="notice-full-item" onclick="togglePageItem('update', ${i}, this)">
            <div class="notice-full-header">
                <span class="notice-text">${u.isNew ? `<span class="badge-n" id="upbadge-${i}">N</span> ` : ''}${u.title}</span>
                <span class="notice-date">${u.date}</span>
            </div>
        </div>`).join('');
};

// 항목 클릭 → 제목·내용을 우측 1/4 화면 상세 패널에 표시
window.togglePageItem = (type, i, el) => {
    const data = type === 'notice' ? ALL_NOTICES[i] : ALL_UPDATES[i];
    if (!data) return;
    const prefix = type === 'notice' ? 'notice' : 'update';

    // 우측 상세 패널 채우기 + 열기
    document.getElementById(prefix + 'DetailDate').textContent    = data.date;
    document.getElementById(prefix + 'DetailTitle').textContent   = data.title;
    document.getElementById(prefix + 'DetailContent').textContent = data.content || '내용이 없습니다.';
    document.getElementById(prefix + 'DetailPanel').classList.add('open');

    // 선택 항목 강조
    const listId = type === 'notice' ? 'noticePageList' : 'updatePageList';
    document.querySelectorAll('#' + listId + ' .notice-full-item').forEach(it => it.classList.remove('selected'));
    el?.classList.add('selected');

    // N 뱃지 제거
    const badgeId = type === 'notice' ? `npbadge-${i}` : `upbadge-${i}`;
    if (data.isNew) {
        data.isNew = false;
        document.getElementById(badgeId)?.remove();
        (type === 'notice' ? renderNoticeCard : renderUpdateCard)();
    }
};

// 우측 상세 패널 닫기
window.closeNuDetail = (type) => {
    const prefix = type === 'notice' ? 'notice' : 'update';
    document.getElementById(prefix + 'DetailPanel')?.classList.remove('open');
    const listId = type === 'notice' ? 'noticePageList' : 'updatePageList';
    document.querySelectorAll('#' + listId + ' .notice-full-item.selected')
        .forEach(it => it.classList.remove('selected'));
};

window.updateNavIndicator = () => {
    const activeEl = document.querySelector('.nav-item.active');
    const indicator = document.getElementById('navIndicator');
    const container = document.getElementById('navLinks');
    if (!activeEl || !indicator || !container) return;
    const containerLeft = container.getBoundingClientRect().left;
    const rect = activeEl.getBoundingClientRect();
    indicator.style.left  = (rect.left - containerLeft) + 'px';
    indicator.style.width = rect.width + 'px';
};

window.changeClass = (className) => {
    const el = document.getElementById('currentClassName');
    if (el) el.innerText = className;
};

const PREFS_COL = 'teacher_settings';

const saveUserPrefs = (uid, prefs) => {
    setDoc(doc(db, PREFS_COL, uid), prefs, { merge: true })
        .catch(e => console.error('설정 저장 실패:', e));
};

const loadAndApplyPrefs = async (uid) => {
    try {
        const userSnap = await getDoc(doc(legacyFs, 'users', uid));
        if (userSnap.exists()) {
            const teacherName = userSnap.data().name || '';
            localStorage.setItem('edutrack_teacher_name', teacherName);
        }

        const snap = await getDoc(doc(db, PREFS_COL, uid));
        if (!snap.exists()) return;
        const data = snap.data();

        const profile = {
            school:   data.school   || '',
            subject:  data.subject  || '',
            position: data.position || '',
        };
        localStorage.setItem('edutrack_teacher', JSON.stringify(profile));
        syncSchoolName();

        if (data.theme != null && data.theme !== '') {
            localStorage.setItem('edutrack_theme', data.theme);
            applyTheme(parseInt(data.theme));
        } else {
            localStorage.removeItem('edutrack_theme');
        }

        loadScheduleFromFirebase(data);
    } catch (e) { console.error('설정 불러오기 실패:', e); }
};

window.openSettingsModal = () => {
    const user = auth.currentUser;
    if (user) {
        const uid = user.email ? user.email.split('@')[0] : user.uid;
        document.getElementById('s-uid').textContent   = uid;
        document.getElementById('s-email').textContent = user.email || '-';
    }
    const saved = JSON.parse(localStorage.getItem('edutrack_teacher') || '{}');
    const schoolFull = saved.school || '';
    const schoolInput = schoolFull.endsWith(' 고등학교')
        ? schoolFull.slice(0, -5)
        : schoolFull;
    document.getElementById('t-school').value   = schoolInput;
    document.getElementById('t-position').value = saved.position || '';

    const teacherName = localStorage.getItem('edutrack_teacher_name') || '';
    const nameEl = document.getElementById('t-name');
    if (nameEl) nameEl.value = teacherName;
    const cur = localStorage.getItem('edutrack_theme');
    document.querySelectorAll('.theme-swatch').forEach((el, i) => {
        el.classList.toggle('selected', cur !== null && i === parseInt(cur));
    });
    switchSettingsTab('account', document.querySelector('.stab'));
    document.getElementById('settingsModal').style.display = 'flex';
};

window.switchSettingsTab = (tab, btn) => {
    document.querySelectorAll('.stab-content').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.stab').forEach(el => el.classList.remove('active'));
    document.getElementById('stab-' + tab).classList.add('active');
    if (btn) btn.classList.add('active');
    if (tab === 'classcode') window.loadClassCodes?.();
};

window.sendPasswordReset = async () => {
    const user = auth.currentUser;
    if (!user?.email) return alert('이메일 정보를 찾을 수 없습니다.');
    try {
        await sendPasswordResetEmail(auth, user.email);
        alert(`${user.email} 로 비밀번호 재설정 이메일을 발송했습니다.`);
    } catch (e) { alert('발송 실패: ' + e.message); }
};

const THEMES = [
    { active: '#5A78B8', sidebar: '#1A4778' },
    { active: '#649552', sidebar: '#245532' },
    { active: '#E87C52', sidebar: '#A83C32' },
    { active: '#F4B842', sidebar: '#D47822' },
    { active: '#927AB8', sidebar: '#523A78' },
    { active: '#5AB8B8', sidebar: '#1A7878' },
];
const DEFAULT_SIDEBAR = '#1a2332';
const DEFAULT_ACTIVE  = '#3182F6';

const getThemeStyle = () => {
    let el = document.getElementById('theme-override');
    if (!el) {
        el = document.createElement('style');
        el.id = 'theme-override';
        document.head.appendChild(el);
    }
    return el;
};

window.applyTheme = (idx) => {
    const t = THEMES[idx];
    const sidebar = document.querySelector('.sidebar');
    if (sidebar) sidebar.style.backgroundColor = t.sidebar;
    getThemeStyle().textContent = `
        :root { --primary: ${t.active}; }
        .nav-item.active { color: ${t.active} !important; }
        .nav-indicator { background: ${t.active} !important; }
        .topbar-shortcut-btn.active { color: ${t.active} !important; }
        .topbar-shortcut-btn.active::after { background: ${t.active} !important; box-shadow: 0 0 8px ${t.active}88 !important; }
    `;
    localStorage.setItem('edutrack_theme', idx);
    document.querySelectorAll('.theme-swatch').forEach((el, i) => {
        el.classList.toggle('selected', i === idx);
    });
    const user = auth.currentUser;
    if (user) saveUserPrefs(user.uid, { theme: String(idx) });
};

window.resetTheme = () => {
    const sidebar = document.querySelector('.sidebar');
    if (sidebar) sidebar.style.backgroundColor = DEFAULT_SIDEBAR;
    getThemeStyle().textContent = '';
    localStorage.removeItem('edutrack_theme');
    document.querySelectorAll('.theme-swatch').forEach(el => el.classList.remove('selected'));
    const user = auth.currentUser;
    if (user) saveUserPrefs(user.uid, { theme: null });
};

const savedTheme = localStorage.getItem('edutrack_theme');
if (savedTheme !== null) applyTheme(parseInt(savedTheme));

const syncSchoolName = () => {
    const saved  = JSON.parse(localStorage.getItem('edutrack_teacher') || '{}');
    const school = saved.school || '학교 정보 없음';
    const el1 = document.getElementById('homeSchoolName');
    const el2 = document.getElementById('sidebarSchoolName');
    if (el1) el1.textContent = school;
    if (el2) el2.textContent = school;
};
syncSchoolName();

window.saveTeacherInfo = async () => {
    const schoolInput = document.getElementById('t-school').value.trim();
    const teacherName = document.getElementById('t-name').value.trim();
    const data = {
        school:   schoolInput ? schoolInput + ' 고등학교' : '',
        position: document.getElementById('t-position').value.trim(),
    };
    localStorage.setItem('edutrack_teacher', JSON.stringify(data));
    localStorage.setItem('edutrack_teacher_name', teacherName);
    syncSchoolName();
    const user = auth.currentUser;
    if (user) {
        saveUserPrefs(user.uid, data);
        try {
            await setDoc(doc(legacyFs, 'users', user.uid), { name: teacherName }, { merge: true });
        } catch (e) {
            console.error('이름 저장 실패:', e);
        }
    }
    alert('교사정보가 저장되었습니다.');
};

// =====================================================================
// 🔑 학급 코드 관리
// =====================================================================
function _genCode() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // 0/O, 1/I 제외
    return Array.from({ length: 6 }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
}

function _renderCodeCard(code, d) {
    const expiryStr = d.expiresAt ? new Date(d.expiresAt).toLocaleDateString('ko-KR') : '-';
    return `
        <div style="border:1px solid #e2e8f0; border-radius:10px; padding:14px 16px; margin-bottom:10px; background:#fff;">
            <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:8px;">
                <div>
                    <div style="font-size:12px; color:#718096; margin-bottom:4px; font-weight:600;">${d.grade}학년 ${d.class}반</div>
                    <div style="font-size:24px; font-weight:800; letter-spacing:5px; color:#1a202c; font-family:monospace;">${code}</div>
                </div>
                <div style="display:flex; gap:6px; flex-wrap:wrap;">
                    <button onclick="window.copyClassCode('${code}')" style="padding:6px 12px; border:1px solid #e2e8f0; border-radius:6px; font-size:12px; font-weight:600; cursor:pointer; background:#fff; color:#4a5568;">📋 복사</button>
                    <button onclick="window.showQRCode('${code}','${d.grade}학년 ${d.class}반')" style="padding:6px 12px; border:1px solid #e2e8f0; border-radius:6px; font-size:12px; font-weight:600; cursor:pointer; background:#fff; color:#4a5568;">QR</button>
                    <button onclick="window.deactivateClassCode('${code}')" style="padding:6px 12px; border:1px solid #fecaca; border-radius:6px; font-size:12px; font-weight:600; cursor:pointer; background:#fff; color:#e74c3c;">삭제</button>
                </div>
            </div>
            <div style="font-size:11px; color:#a0aec0; margin-top:8px;">만료일: ${expiryStr}</div>
        </div>`;
}

window.loadClassCodes = async () => {
    const listEl = document.getElementById('cc-list');
    if (!listEl) return;
    listEl.innerHTML = '<div style="text-align:center;color:#a0aec0;font-size:13px;padding:24px;">불러오는 중...</div>';
    try {
        const user = auth.currentUser;
        if (!user) return;
        const snap = await getDocs(query(
            collection(db, 'class_codes'),
            where('teacherId', '==', user.uid),
            where('isActive', '==', true)
        ));
        if (snap.empty) {
            listEl.innerHTML = '<div style="text-align:center;color:#a0aec0;font-size:13px;padding:24px 0;">발급된 코드가 없습니다.</div>';
            return;
        }
        // 학년 → 반 순서로 정렬
        const sorted = snap.docs.slice().sort((a, b) => {
            const da = a.data(), db2 = b.data();
            return (+da.grade - +db2.grade) || (+da.class - +db2.class);
        });
        listEl.innerHTML = '';
        sorted.forEach(docSnap => {
            listEl.insertAdjacentHTML('beforeend', _renderCodeCard(docSnap.id, docSnap.data()));
        });
    } catch (e) {
        listEl.innerHTML = `<div style="text-align:center;color:#e74c3c;font-size:13px;padding:24px;">불러오기 실패: ${e.message}</div>`;
    }
};

window.issueClassCode = async () => {
    const grade = document.getElementById('cc-grade').value;
    const cls   = document.getElementById('cc-class').value;
    if (!grade || !cls) return alert('학년과 반을 선택해주세요.');

    const user = auth.currentUser;
    if (!user) return;

    // 같은 반 기존 코드 확인
    const existing = await getDocs(query(
        collection(db, 'class_codes'),
        where('teacherId', '==', user.uid),
        where('grade', '==', grade),
        where('class', '==', cls),
        where('isActive', '==', true)
    ));
    if (!existing.empty) {
        const ok = confirm(`${grade}학년 ${cls}반의 기존 코드가 있습니다.\n새로 발급하면 기존 코드는 삭제됩니다. 계속하시겠습니까?`);
        if (!ok) return;
        for (const d of existing.docs) {
            await setDoc(doc(db, 'class_codes', d.id), { isActive: false }, { merge: true });
        }
    }

    const code = _genCode();
    const teacherName = localStorage.getItem('edutrack_teacher_name') || '';
    const teacherInfo = JSON.parse(localStorage.getItem('edutrack_teacher') || '{}');
    const expiresAt   = new Date(new Date().getFullYear(), 11, 31).getTime(); // 올해 12/31

    await setDoc(doc(db, 'class_codes', code), {
        grade,
        class:       cls,
        teacherId:   user.uid,
        teacherName,
        school:      teacherInfo.school || '',
        createdAt:   Date.now(),
        expiresAt,
        isActive:    true
    });

    await window.loadClassCodes();
    alert(`✅ ${grade}학년 ${cls}반 코드가 발급되었습니다!\n\n코드:  ${code}\n\n학생들에게 이 코드를 알려주세요.`);
};

window.deactivateClassCode = async (code) => {
    if (!confirm(`코드 [${code}]를 삭제하시겠습니까?\n삭제 후 이 코드로는 회원가입이 불가능합니다.`)) return;
    await setDoc(doc(db, 'class_codes', code), { isActive: false }, { merge: true });
    await window.loadClassCodes();
};

window.copyClassCode = (code) => {
    navigator.clipboard.writeText(code)
        .then(() => alert(`코드 [${code}]가 클립보드에 복사되었습니다.`))
        .catch(() => alert(`코드: ${code}`));
};

window.showQRCode = (code, label) => {
    document.getElementById('qrCodeOverlay')?.remove();
    const overlay = document.createElement('div');
    overlay.id = 'qrCodeOverlay';
    overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.6);z-index:10000;display:flex;align-items:center;justify-content:center;';
    overlay.innerHTML = `
        <div style="background:#fff;border-radius:16px;padding:28px 32px;text-align:center;max-width:280px;width:90%;">
            <div style="font-size:15px;font-weight:700;color:#1a202c;margin-bottom:4px;">${label}</div>
            <div style="font-size:11px;color:#718096;margin-bottom:16px;">학생 앱 회원가입 코드</div>
            <img src="https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=EDUTRACK:${code}"
                 style="width:180px;height:180px;border-radius:8px;border:1px solid #e2e8f0;" />
            <div style="font-size:22px;font-weight:800;letter-spacing:6px;color:#1a202c;font-family:monospace;margin-top:14px;">${code}</div>
            <div style="font-size:11px;color:#a0aec0;margin-top:6px;">앱 회원가입 화면에서 이 코드를 입력하세요</div>
            <button onclick="document.getElementById('qrCodeOverlay').remove()"
                style="margin-top:18px;padding:9px 28px;background:#e2e8f0;border:none;border-radius:8px;font-size:13px;font-weight:600;cursor:pointer;color:#4a5568;">닫기</button>
        </div>`;
    overlay.addEventListener('click', e => { if (e.target === overlay) overlay.remove(); });
    document.body.appendChild(overlay);
};

let rosterAllStudents = [];
let rosterCurrentGrade = 1;
let rosterCurrentClass = null;
let rosterRenderedStudents = []; // 현재 표에 그려진 학생 목록 (행 클릭 시 인덱스로 참조)

const rosterFromUsers    = new Map(); 
const rosterFromStudents = new Map(); 
const rosterFromScores   = new Map(); 

const mergeRosterData = () => {
    const merged = new Map();
    rosterFromUsers.forEach((v, k) => merged.set(k, { ...v }));
    rosterFromScores.forEach((v, k) => merged.set(k, { ...merged.get(k), ...v }));
    rosterFromStudents.forEach((v, k) => merged.set(k, { ...merged.get(k), ...v }));
    let all = Array.from(merged.values());

    // 번호 있는 항목이 존재하면 같은 이름+학년+반의 번호 없는 항목 제거 (구 계정 중복 방지)
    const withNumber = new Set(
        all.filter(s => s.number > 0)
           .map(s => `${s.grade}_${s.class}_${s.name}`)
    );
    rosterAllStudents = all.filter(s =>
        s.number > 0 || !withNumber.has(`${s.grade}_${s.class}_${s.name}`)
    );

    if (document.getElementById('rosterView')?.classList.contains('active')) {
        renderRosterClassTabs();
        renderRosterStudents();
    }
};

const getRosterClasses = (grade) => {
    const set = new Set();
    rosterAllStudents
        .filter(s => String(s.grade) === String(grade))
        .forEach(s => { if (s.class) set.add(String(s.class)); });
    return Array.from(set).sort((a, b) => parseInt(a) - parseInt(b));
};

const renderRosterGradeTabs = () => {
    document.querySelectorAll('.roster-gtab').forEach((btn, i) => {
        btn.classList.toggle('active', i + 1 === rosterCurrentGrade);
    });
};

const renderRosterClassTabs = () => {
    const bar     = document.getElementById('rosterClassTabs');
    if (!bar) return;
    const classes = getRosterClasses(rosterCurrentGrade);

    if (!classes.length) {
        bar.innerHTML = '<span style="color:#a0aec0; font-size:13px;">해당 학년 학생 없음</span>';
        rosterCurrentClass = null;
        return;
    }
    if (!classes.includes(String(rosterCurrentClass))) rosterCurrentClass = classes[0];

    bar.innerHTML = classes.map(c => `
        <button class="roster-ctab ${String(c) === String(rosterCurrentClass) ? 'active' : ''}"
                onclick="selectRosterClass('${c}')">${c}반</button>
    `).join('');
};

const renderRosterStudents = () => {
    const title = document.getElementById('rosterListTitle');
    const tbody = document.getElementById('rosterStudentBody');
    if (!title || !tbody) return;

    if (!rosterCurrentClass) {
        title.textContent = `${rosterCurrentGrade}학년`;
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:#a0aec0; padding:30px;">반을 선택해주세요.</td></tr>';
        return;
    }

    const students = rosterAllStudents
        .filter(s => String(s.grade) === String(rosterCurrentGrade) && String(s.class) === String(rosterCurrentClass))
        .sort((a, b) => {
            // 번호 있는 학생을 번호 오름차순으로 먼저, 번호 없는 학생은 뒤에 이름순으로
            const na = Number(a.number) || 0;
            const nb = Number(b.number) || 0;
            if (na && nb) return na - nb;
            if (na) return -1;
            if (nb) return 1;
            return (a.name || '').localeCompare(b.name || '', 'ko');
        });

    title.innerHTML = `${rosterCurrentGrade}학년 ${rosterCurrentClass}반 <span style="font-size:13px; color:#718096; font-weight:400;">총 ${students.length}명</span>`;

    if (!students.length) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:#a0aec0; padding:30px;">해당 반 학생 없음</td></tr>';
        return;
    }

    rosterRenderedStudents = students;
    tbody.innerHTML = students.map((s, i) => `
        <tr class="roster-student-row" data-idx="${i}">
            <td style="color:#718096;">${s.number ? s.number + '번' : i + 1}</td>
            <td><strong>${s.name || '-'}</strong></td>
        </tr>`).join('');
};

// 학생 행 클릭 → 상세 패널 (이벤트 위임, 1회 등록 · 이름에 특수문자가 있어도 안전)
document.getElementById('rosterStudentBody')?.addEventListener('click', e => {
    const row = e.target.closest('.roster-student-row[data-idx]');
    if (!row) return;
    const s = rosterRenderedStudents[parseInt(row.dataset.idx)];
    if (!s) return;
    window.openStudentPanel(s.name, s.grade, s.class, s.number || 0);
});

window.openStudentPanel = async (name, grade, cls, number = 0) => {
    document.getElementById('panelStudentName').textContent = name;
    document.getElementById('panelStudentMeta').textContent = `${grade}학년 ${cls}반${number ? ' ' + number + '번' : ''}`;
    document.getElementById('studentDetailPanel').classList.add('open');

    const body = document.getElementById('panelStudentBody');
    body.innerHTML = `<div style="color:var(--text-5);font-size:13px;text-align:center;padding:24px 0;">불러오는 중...</div>`;

    try {
        const snap = await getDocs(query(
            collection(db, 'student_scores'),
            where('name', '==', name)
        ));

        const records = [];
        snap.forEach(d => {
            const data = d.data();
            if (String(data.grade) !== String(grade) || String(data.class) !== String(cls)) return;
            // number가 둘 다 있을 때만 번호로 구분 (한쪽이 0이면 같은 학생으로 간주)
            if (number > 0 && data.number > 0 && data.number !== number) return;
            records.push(data);
        });
        records.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));

        if (!records.length) {
            body.innerHTML = `<div style="color:var(--text-5);font-size:13px;text-align:center;padding:24px 0;">응시한 시험이 없습니다.</div>`;
            return;
        }

        body.innerHTML = records.map(r => {
            const isCheating = r.score === '부정행위';
            const scoreColor = isCheating ? '#e53e3e' : r.score >= 80 ? '#38a169' : r.score >= 60 ? '#d97706' : '#e53e3e';
            const scoreBg    = isCheating ? '#fff5f5' : r.score >= 80 ? '#f0fff4' : r.score >= 60 ? '#fffbeb' : '#fff5f5';
            const scoreLabel = isCheating ? '부정행위' : `${r.score}점`;
            const keywords   = Array.isArray(r.keywords) ? r.keywords.join(' ') : '';
            const dateStr    = r.timestamp ? new Date(r.timestamp).toLocaleDateString('ko-KR') : '';
            const subjectEsc = (r.subject || '일반 평가').replace(/"/g, '&quot;');
            const clickable  = !isCheating ? `data-clickable data-name="${name}" data-score="${r.score}" data-subject="${subjectEsc}"` : '';
            return `
                <div class="student-panel-record" ${clickable}>
                    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:5px;">
                        <div class="record-subject" style="font-size:13px;font-weight:600;color:var(--text-1);flex:1;margin-right:8px;">${r.subject || '일반 평가'}</div>
                        <span style="background:${scoreBg};color:${scoreColor};padding:3px 9px;border-radius:10px;font-size:12px;font-weight:700;white-space:nowrap;">${scoreLabel}</span>
                    </div>
                    ${keywords ? `<div style="font-size:12px;color:var(--text-4);margin-bottom:3px;">${keywords}</div>` : ''}
                    ${dateStr  ? `<div style="font-size:11px;color:var(--text-5);">${dateStr}</div>` : ''}
                    ${!isCheating ? `<div style="font-size:11px;color:var(--primary);margin-top:5px;">탭하여 분석 리포트 보기 →</div>` : ''}
                </div>`;
        }).join('');
    } catch (e) {
        body.innerHTML = `<div style="color:#e53e3e;font-size:13px;padding:16px 0;">불러오기 실패: ${e.message}</div>`;
    }
};

window.closeStudentPanel = () => {
    document.getElementById('studentDetailPanel')?.classList.remove('open');
};

// 패널 내 시험 클릭 → 분석 리포트 모달 (이벤트 위임, 1회 등록)
document.getElementById('studentDetailPanel')?.addEventListener('click', e => {
    const record = e.target.closest('.student-panel-record[data-clickable]');
    if (!record) return;
    window.openStudentInfoModal?.(
        record.dataset.name,
        parseFloat(record.dataset.score) || 0,
        record.dataset.subject || '일반 평가'
    );
});

window.selectRosterGrade = (grade) => {
    rosterCurrentGrade = grade;
    rosterCurrentClass = null;
    renderRosterGradeTabs();
    renderRosterClassTabs();
    renderRosterStudents();
};

window.selectRosterClass = (cls) => {
    rosterCurrentClass = cls;
    renderRosterClassTabs();
    renderRosterStudents();
};

window.showRosterView = (grade) => {
    window.showView('rosterView');
    window.selectRosterGrade(grade);
};

const periodRowMap = [0, 1, 2, 3, 5, 6, 7];

window.openAddTimeModal = () => {
    document.getElementById('addTimeModal').style.display = 'flex';
};

window.submitAddTime = () => {
    const periodIdx = parseInt(document.getElementById('addTimePeriod').value);
    const dayColIdx = parseInt(document.getElementById('addTimeDay').value);
    const subject   = document.getElementById('addTimeSubject').value.trim();
    const cls       = document.getElementById('addTimeClass').value.trim();

    if (!subject) { alert('과목명을 입력해주세요.'); return; }

    const tbody = document.querySelector('.timetable tbody');
    const rows  = tbody.querySelectorAll('tr');
    const targetRow  = rows[periodRowMap[periodIdx]];
    if (!targetRow) return;
    const targetCell = targetRow.cells[dayColIdx];
    if (!targetCell) return;

    const label = cls ? `${subject}<br><small>${cls}</small>` : subject;
    targetCell.innerHTML = `<div class="class-cell">${label}</div>`;

    document.getElementById('addTimeSubject').value = '';
    document.getElementById('addTimeClass').value   = '';
    window.closeModal('addTimeModal');
    window.saveScheduleToFirebase?.();
};

// ── 시간 선택 셀렉트 옵션 생성 (시: 0-23, 분: 5분 단위) ──
const buildTimeSelectOptions = (hId, mId, h = 9, m = 0) => {
    const hEl = document.getElementById(hId);
    const mEl = document.getElementById(mId);
    if (!hEl || !mEl) return;
    hEl.innerHTML = Array.from({ length: 24 }, (_, i) =>
        `<option value="${i}"${i === h ? ' selected' : ''}>${String(i).padStart(2, '0')}</option>`
    ).join('');
    mEl.innerHTML = Array.from({ length: 12 }, (_, i) => {
        const min = i * 5;
        return `<option value="${min}"${min === m ? ' selected' : ''}>${String(min).padStart(2, '0')}</option>`;
    }).join('');
};

// 기존 시간 문자열 파싱 → {allDay, startH, startM, endH, endM, hasEnd}
const parseFixedTimeStr = (timeStr) => {
    if (!timeStr || timeStr === '하루종일') return { allDay: true };
    const parts = timeStr.split(/[-–]/);
    const [sh, sm] = parts[0].trim().split(':').map(Number);
    if (parts.length > 1) {
        const [eh, em] = parts[1].trim().split(':').map(Number);
        return { allDay: false, startH: sh || 0, startM: sm || 0, endH: eh || 0, endM: em || 0, hasEnd: true };
    }
    return { allDay: false, startH: sh || 0, startM: sm || 0, hasEnd: false };
};

// 시간 선택 UI에서 시간 문자열 조합
const readFixedTimeFromUI = (prefix) => {
    if (document.getElementById(`${prefix}AllDay`)?.checked) return '하루종일';
    const sh = String(document.getElementById(`${prefix}StartH`).value).padStart(2, '0');
    const sm = String(document.getElementById(`${prefix}StartM`).value).padStart(2, '0');
    if (document.getElementById(`${prefix}EndCheck`)?.checked) {
        const eh = String(document.getElementById(`${prefix}EndH`).value).padStart(2, '0');
        const em = String(document.getElementById(`${prefix}EndM`).value).padStart(2, '0');
        return `${sh}:${sm}-${eh}:${em}`;
    }
    return `${sh}:${sm}`;
};

// 하루종일 토글
window.toggleAddFixedAllDay = (cb) => {
    document.getElementById('addFixedTimeArea').style.display = cb.checked ? 'none' : 'block';
};
window.toggleEditFixedAllDay = (cb) => {
    document.getElementById('editFixedTimeArea').style.display = cb.checked ? 'none' : 'block';
};
// 종료 시간 영역 토글 (add/edit 공용)
window.toggleFixedEndArea = (cb, areaId) => {
    const area = document.getElementById(areaId);
    if (area) area.style.display = cb.checked ? 'flex' : 'none';
};

window.openAddFixedModal = () => {
    document.getElementById('addFixedAllDay').checked  = false;
    document.getElementById('addFixedEndCheck').checked = false;
    document.getElementById('addFixedTimeArea').style.display  = 'block';
    document.getElementById('addFixedEndArea').style.display   = 'none';
    buildTimeSelectOptions('addFixedStartH', 'addFixedStartM', 9, 0);
    buildTimeSelectOptions('addFixedEndH',   'addFixedEndM',   10, 0);
    document.getElementById('addFixedModal').style.display = 'flex';
};

window.submitAddFixed = () => {
    const title    = document.getElementById('addFixedTitle').value.trim();
    const day      = document.getElementById('addFixedDay').value;
    const location = document.getElementById('addFixedLocation').value.trim();
    const time     = readFixedTimeFromUI('addFixed');

    if (!title) { alert('일정명을 입력해주세요.'); return; }

    const container = document.querySelector('.fixed-schedule-card');
    if (!container) return;

    const prevLast = container.querySelector('.fixed-item.last');
    if (prevLast) prevLast.classList.remove('last');

    const item = document.createElement('div');
    item.className = 'fixed-item last';
    item.innerHTML = `
        <div class="fixed-title">${title}</div>
        <div class="fixed-detail">${day} ${time}</div>
        ${location ? `<div class="fixed-location">${location}</div>` : ''}
    `;
    container.appendChild(item);

    document.getElementById('addFixedTitle').value    = '';
    document.getElementById('addFixedLocation').value = '';
    window.closeModal('addFixedModal');
    window.saveScheduleToFirebase?.();
};

window.closeModal = (id) => {
    const target = document.getElementById(id);
    if (target) target.style.display = 'none';
};

// =====================================================================
// 📅 교사 일정 Firebase 저장 / 불러오기
// =====================================================================
window.saveScheduleToFirebase = async () => {
    const user = auth.currentUser;
    if (!user) return;

    // 시간표 직렬화: cells 프로퍼티 기준, 점심행 제외, X버튼 clone으로 제거 후 읽기
    const ttData = {};
    const tbody = document.querySelector('.timetable tbody');
    if (tbody) {
        tbody.querySelectorAll('tr').forEach((tr, rowIdx) => {
            if (tr.classList.contains('lunch-row')) return;
            for (let colIdx = 2; colIdx < tr.cells.length; colIdx++) {
                const td = tr.cells[colIdx];
                if (!td) continue;
                const clone = td.cloneNode(true);
                clone.querySelectorAll('.delete-x-btn').forEach(b => b.remove());
                const cell = clone.querySelector('.class-cell');
                const html = cell?.innerHTML?.trim() || '';
                if (html) ttData[`${rowIdx}_${colIdx}`] = html;
            }
        });
    }

    // 고정 일정 직렬화
    const fsData = [];
    document.querySelectorAll('.fixed-schedule-card .fixed-item').forEach(item => {
        const title    = item.querySelector('.fixed-title')?.textContent?.trim()    || '';
        const detail   = item.querySelector('.fixed-detail')?.textContent?.trim()   || '';
        const location = item.querySelector('.fixed-location')?.textContent?.trim() || '';
        if (title) fsData.push({ title, detail, location });
    });
    window.fixedScheduleData = fsData;

    // mergeFields로 timetable·fixedSchedule 필드만 완전 교체 (딥 머지 방지)
    // 빈 시간표는 _cleared 센티널로 "의도적 삭제" 표시
    const timetableToSave = Object.keys(ttData).length ? ttData : { _cleared: true };
    try {
        await setDoc(
            doc(db, PREFS_COL, user.uid),
            { timetable: timetableToSave, fixedSchedule: fsData },
            { mergeFields: ['timetable', 'fixedSchedule'] }
        );
    } catch (e) {
        console.error('일정 저장 실패:', e);
    }
    renderNextClass();
    renderUpcomingSchedule();
};

const loadScheduleFromFirebase = (data) => {
    // 시간표 복원 — undefined면 저장된 적 없음(기본값 유지), 그 외엔 항상 적용
    if (data.timetable !== undefined) {
        const tbody = document.querySelector('.timetable tbody');
        if (tbody) {
            tbody.querySelectorAll('tr').forEach(tr => {
                [...tr.querySelectorAll('td')].forEach((td, colIdx) => {
                    if (colIdx >= 2) td.innerHTML = '';
                });
            });
            Object.entries(data.timetable || {}).forEach(([key, html]) => {
                if (key.startsWith('_')) return; // 센티널 키 무시
                const [rowIdx, colIdx] = key.split('_').map(Number);
                const rows = tbody.querySelectorAll('tr');
                const td = rows[rowIdx]?.cells[colIdx];
                if (td) td.innerHTML = `<div class="class-cell">${html}</div>`;
            });
        }
    }

    // 고정 일정 복원 — undefined면 기본값 유지, 빈 배열이면 전체 삭제 반영
    if (data.fixedSchedule !== undefined) {
        const items = Array.isArray(data.fixedSchedule) ? data.fixedSchedule : [];
        window.fixedScheduleData = items;
        const container = document.querySelector('.fixed-schedule-card');
        if (container) {
            container.innerHTML = items.map((item, i) => `
                <div class="fixed-item ${i === items.length - 1 ? 'last' : ''}">
                    <div class="fixed-title">${item.title}</div>
                    <div class="fixed-detail">${item.detail}</div>
                    ${item.location ? `<div class="fixed-location">${item.location}</div>` : ''}
                </div>
            `).join('');
        }
    }

    renderNextClass();
    renderUpcomingSchedule();
};

// =====================================================================
// 🏠 대시보드 홈 — 다음 수업 시간 / 다가오는 일정 동적 렌더링
// =====================================================================
const PERIOD_INFO = [
    { h: 9,  m: 0,  time: '09:00-09:50', label: '1교시' },
    { h: 10, m: 0,  time: '10:00-10:50', label: '2교시' },
    { h: 11, m: 0,  time: '11:00-11:50', label: '3교시' },
    { h: 12, m: 0,  time: '12:00-12:50', label: '4교시' },
    null,                                                    // 점심
    { h: 13, m: 50, time: '13:50-14:40', label: '5교시' },
    { h: 14, m: 50, time: '14:50-15:40', label: '6교시' },
    { h: 15, m: 50, time: '15:50-16:40', label: '7교시' },
];
const COL_TO_JSDAY = { 2: 1, 3: 2, 4: 3, 5: 4, 6: 5 };
const DAY_KO = ['일', '월', '화', '수', '목', '금', '토'];

const renderNextClass = () => {
    const el = document.getElementById('nextClassBody');
    if (!el) return;

    const tbody = document.querySelector('.timetable tbody');
    if (!tbody) return;

    const classes = [];
    tbody.querySelectorAll('tr').forEach((tr, rowIdx) => {
        const period = PERIOD_INFO[rowIdx];
        if (!period) return;
        [...tr.querySelectorAll('td')].forEach((td, colIdx) => {
            const jsDay = COL_TO_JSDAY[colIdx];
            if (!jsDay) return;
            const cell = td.querySelector('.class-cell');
            if (!cell) return;
            const tmp = document.createElement('div');
            tmp.innerHTML = cell.innerHTML;
            const subject = tmp.childNodes[0]?.textContent?.trim() || tmp.textContent.trim();
            const cls     = tmp.querySelector('small')?.textContent?.trim() || '';
            classes.push({ jsDay, subject, cls, ...period });
        });
    });

    if (!classes.length) {
        el.innerHTML = `<div style="text-align:center;color:var(--text-5);padding:20px 0;font-size:13px;">등록된 수업이 없습니다.<br><small>교사 일정 탭에서 시간표를 추가하세요.</small></div>`;
        return;
    }

    const now = new Date();
    const curDay  = now.getDay();
    const curMins = now.getHours() * 60 + now.getMinutes();

    let next = null;
    for (let offset = 0; offset <= 7 && !next; offset++) {
        const checkDay = (curDay + offset) % 7;
        if (checkDay === 0 || checkDay === 6) continue;
        const todays = classes
            .filter(c => c.jsDay === checkDay)
            .sort((a, b) => (a.h * 60 + a.m) - (b.h * 60 + b.m));
        for (const c of todays) {
            if (offset > 0 || c.h * 60 + c.m > curMins) { next = { ...c, offset }; break; }
        }
    }

    if (!next) {
        el.innerHTML = `<div style="text-align:center;color:var(--text-5);padding:20px 0;font-size:13px;">이번 주 남은 수업이 없습니다.</div>`;
        return;
    }

    const when = next.offset === 0 ? '오늘' : next.offset === 1 ? '내일' : `${DAY_KO[next.jsDay]}요일`;
    el.innerHTML = `
        <div class="next-class-subject">${next.subject}</div>
        ${next.cls ? `<div class="next-class-grade">${next.cls}</div>` : ''}
        <div class="next-class-meta">시간: ${next.time}</div>
        <div class="next-class-meta">${next.label} · ${when}</div>
    `;
};

const renderUpcomingSchedule = () => {
    const el = document.getElementById('upcomingList');
    if (!el) return;

    const data = window.fixedScheduleData;
    if (!data.length) {
        el.innerHTML = `<div style="color:var(--text-5);font-size:13px;text-align:center;padding:8px 0;">등록된 일정이 없습니다.<br><small>교사 일정 탭에서 고정 일정을 추가하세요.</small></div>`;
        return;
    }

    const now     = getKST();
    const curDay  = now.getDay();
    const curMins = now.getHours() * 60 + now.getMinutes();
    const DAY_MAP = { '월': 1, '화': 2, '수': 3, '목': 4, '금': 5 };
    const DOT_CLS = ['dot-red', 'dot-blue', 'dot-green'];

    const upcoming = [];
    data.forEach(({ title, detail, location }) => {
        const isAllDay = detail.includes('하루종일');
        const timeM    = isAllDay ? null : detail.match(/(\d{1,2}:\d{2})/);
        if (!isAllDay && !timeM) return;

        const timeStr  = isAllDay ? '하루종일' : timeM[1];
        const timeMins = isAllDay ? 0 : (() => { const [h, m] = timeStr.split(':').map(Number); return h * 60 + m; })();

        Object.entries(DAY_MAP)
            .filter(([name]) => detail.includes(name))
            .forEach(([, jsDay]) => {
                for (let offset = 0; offset <= 7; offset++) {
                    if ((curDay + offset) % 7 === jsDay) {
                        if (isAllDay || offset > 0 || timeMins > curMins) {
                            upcoming.push({ title, timeStr, location, jsDay, offset, timeMins });
                            break;
                        }
                    }
                }
            });
    });

    upcoming.sort((a, b) => a.offset !== b.offset ? a.offset - b.offset : a.timeMins - b.timeMins);
    const top3 = upcoming.slice(0, 3);

    if (!top3.length) {
        el.innerHTML = `<div style="color:var(--text-5);font-size:13px;text-align:center;padding:8px 0;">이번 주 남은 일정이 없습니다.</div>`;
        return;
    }

    el.innerHTML = top3.map((item, i) => {
        const when     = item.offset === 0 ? '오늘' : item.offset === 1 ? '내일' : `${DAY_KO[item.jsDay]}요일`;
        const timeLabel = item.timeStr === '하루종일' ? '하루종일' : item.timeStr;
        return `
            <div class="upcoming-item">
                <div class="upcoming-name"><span class="dot ${DOT_CLS[i]}"></span>${item.title}</div>
                <div class="upcoming-detail">${when} · ${timeLabel}</div>
                ${item.location ? `<div class="upcoming-detail">${item.location}</div>` : ''}
            </div>`;
    }).join('');
};

// =====================================================================
// ✏️ 시험 수정 / 🗑️ 시험 삭제
// =====================================================================
let editingExamId = null;

window.openEditExamModal = async (examId) => {
    editingExamId = examId;
    try {
        const snap = await getDoc(doc(db, 'tests', examId));
        if (!snap.exists()) { alert('시험 데이터를 불러올 수 없습니다.'); return; }
        const d = snap.data();

        document.getElementById('edit-subject').value       = d.subject || '';
        document.getElementById('edit-targetGrade').value   = String(d.grade || '1');
        document.getElementById('edit-timeLimitSelect').value = String(d.timeLimit ?? 0);

        const answers = d.answers || {};
        const qCount  = Object.keys(answers).length || 5;
        const container = document.getElementById('edit-omr-container');
        container.innerHTML = '';
        for (let i = 1; i <= qCount; i++) {
            const row = document.createElement('div');
            row.style.cssText = 'display:flex; align-items:center; margin-bottom:10px; padding-bottom:10px; border-bottom:1px solid #ddd;';
            let html = `<span style="width:50px; font-weight:bold; color:var(--primary);">${i}번.</span>`;
            for (let num = 1; num <= 5; num++) {
                const checked = String(answers[i]) === String(num) ? 'checked' : '';
                html += `<label style="margin-right:15px; cursor:pointer; display:flex; align-items:center;"><input type="radio" name="edit-q${i}" value="${num}" ${checked} style="margin-right:5px;"> ${num}</label>`;
            }
            row.innerHTML = html;
            container.appendChild(row);
        }

        document.getElementById('editExamModal').style.display = 'flex';
    } catch (e) { alert('불러오기 실패: ' + e.message); }
};

window.saveEditExam = async () => {
    if (!editingExamId) return;
    const subject   = document.getElementById('edit-subject').value.trim();
    const grade     = document.getElementById('edit-targetGrade').value;
    const timeLimit = parseInt(document.getElementById('edit-timeLimitSelect').value);
    if (!subject) { alert('과목명을 입력해주세요.'); return; }

    const snap = await getDoc(doc(db, 'tests', editingExamId));
    const qCount = Object.keys(snap.data()?.answers || {}).length;
    const answers = {};
    for (let i = 1; i <= qCount; i++) {
        const sel = document.querySelector(`input[name="edit-q${i}"]:checked`);
        if (!sel) { alert(`${i}번 문항의 정답을 선택해주세요.`); return; }
        answers[i] = sel.value;
    }

    const btn = document.getElementById('editExamSaveBtn');
    btn.textContent = '저장 중...'; btn.disabled = true;
    try {
        await updateDoc(doc(db, 'tests', editingExamId), { subject, grade, timeLimit, answers });
        alert('수정되었습니다.');
        closeModal('editExamModal');
    } catch (e) { alert('수정 실패: ' + e.message); }
    finally { btn.textContent = '저장하기'; btn.disabled = false; }
};

window.deleteExam = async (examId, subject) => {
    if (!confirm(`[${subject}] 시험을 삭제하시겠습니까?\n관련 배포 정보도 함께 삭제됩니다.`)) return;
    try {
        const depSnap = await getDocs(query(collection(db, 'deployments'), where('testId', '==', examId)));
        for (const d of depSnap.docs) await deleteDoc(doc(db, 'deployments', d.id));
        await deleteDoc(doc(db, 'tests', examId));
        alert('삭제되었습니다.');
    } catch (e) { alert('삭제 실패: ' + e.message); }
};

// ESC 키로 모달 닫기 (시험지 업로드 evalModal 제외)
document.addEventListener('keydown', e => {
    if (e.key !== 'Escape') return;
    const open = [...document.querySelectorAll('.modal')].find(m => m.style.display === 'flex');
    if (open && open.id !== 'evalModal') window.closeModal(open.id);
});

// 모달 바깥(배경) 클릭으로 닫기 (시험지 업로드 evalModal 제외)
document.addEventListener('click', e => {
    if (!e.target.classList.contains('modal')) return;
    if (e.target.id === 'evalModal') return;
    window.closeModal(e.target.id);
});

// =====================================================================
// 🌟 1. AI 통문장 추출 알고리즘 (NPL 시뮬레이션 - 20~30문항 통째 분석용)
// =====================================================================
window.extractKeywordsFromText = (text) => {
    if (!text || text.trim() === '') return null;

    // 쉼표 또는 줄바꿈으로 구분된 직접 입력 키워드 우선 처리
    const directItems = text.split(/[,\n]/).map(s => s.trim()).filter(s => s.length >= 2);
    if (directItems.length >= 5) return directItems.slice(0, 5);
    if (directItems.length >= 2 && text.length < 200) return [
        ...directItems,
        ...['개념 이해도', '원리 적용력', '문제 해결력', '추론 및 분석', '자료 해석력']
    ].slice(0, 5);

    let keywords = [];
    
    // 💡 스마트 매칭 (텍스트 내용 안에 아래 단어들이 잡히면 핵심 키워드로 뽑아냅니다)
    if (text.includes("화학식") || text.includes("물") || text.includes("CO2")) keywords.push("기초 화학");
    if (text.includes("수도") || text.includes("미국") || text.includes("워싱턴")) keywords.push("세계 지리");
    if (text.includes("혈액") || text.includes("장기") || text.includes("심장")) keywords.push("인체 생리");
    if (text.includes("훈민정음") || text.includes("조선의 왕") || text.includes("세종")) keywords.push("한국사 인물");
    if (text.includes("WWW") || text.includes("인터넷") || text.includes("웹")) keywords.push("IT 상식");
    if (text.includes("포유류") || text.includes("새끼") || text.includes("젖")) keywords.push("기초 생물");
    if (text.includes("구구단") || text.includes("곱하기")) keywords.push("기초 산술");
    
    if (text.includes("데이터 링크") || text.includes("MAC")) keywords.push("MAC 계층");
    if (text.includes("네트워크 대역") || text.includes("최적의 경로") || text.includes("IP")) keywords.push("IP 라우팅");
    if (text.includes("도메인") || text.includes("DNS")) keywords.push("DNS 도메인");
    if (text.includes("서브넷") || text.includes("DHCP")) keywords.push("DHCP 할당");
    if (text.includes("불법적인 접근") || text.includes("방화벽")) keywords.push("방화벽 보안");

    if (keywords.length >= 5) {
        return keywords.slice(0, 5);
    }

    // 스마트 매칭으로 5개가 안 채워지면, 텍스트 자체의 명사를 잘라내서 무작위로 빈자리를 채웁니다.
    let cleaned = text.replace(/[0-9①②③④⑤\.\,\?\!\(\)\[\]\n℃'"]/g, ' ');
    let words = cleaned.split(/\s+/).map(w => {
        return w.replace(/(은|는|이|가|을|를|의|에서|에|로|으로|과|와|부터|까지|일까요|인가요|무엇|어디|다음|중|설명|정답|모두|고르시오|아닌|대한|하는|해주는|작동하며|기반으로|찾아|무엇일까요|어디일까요|누구일까요|포함되지|위해)$/g, '');
    }).filter(w => w.length >= 2); 

    const stopWords = ['포함', '기준', '없음', '그렇다면', '우리', '포함되지', '않는', '새끼', '기반', '찾아', '연결', '입력', '전송하는', '장비는', '대하여', '이루어진', '관한', '가장', '옳은', '틀린'];
    words = words.filter(w => !stopWords.includes(w));

    let uniqueWords = [...new Set(words)]; 
    
    for (let word of uniqueWords) {
        if (keywords.length >= 5) break;
        if (!keywords.includes(word)) keywords.push(word);
    }

    // 그래도 안 채워지면 땜빵용 기본 키워드 투입
    const backup = ['개념 이해도', '원리 적용력', '문제 해결력', '추론 및 분석', '자료 해석력'];
    for (let word of backup) {
        if (keywords.length >= 5) break;
        if (!keywords.includes(word)) keywords.push(word);
    }

    return keywords;
};

// 보험용 기본 키워드 함수
const getDynamicCategories = (subjectName) => {
    const name = subjectName || "";
    if (name.includes('수학')) return ['대수(방정식)', '기하(도형)', '미적분 기초', '확률과 통계', '수리 논리력'];
    if (name.includes('국어')) return ['현대 문학', '고전 문학', '비문학 독해', '문법과 규범', '어휘력'];
    if (name.includes('영어')) return ['어휘/숙어', '구문/문법', '주제 추론', '빈칸 추론', '듣기/회화'];
    return ['개념 이해도', '원리 적용력', '문제 해결력', '추론 및 분석', '자료 해석력'];
};

window.generateTeacherOMR = (count) => {
    const container = document.getElementById('omr-container');
    container.innerHTML = '';
    for (let i = 1; i <= count; i++) {
        const row = document.createElement('div');
        row.style = "display:flex; align-items:center; margin-bottom:10px; padding-bottom:10px; border-bottom:1px solid #ddd;";
        let radioHtml = `<span style="width:50px; font-weight:bold; color:var(--primary);">${i}번.</span>`;
        for (let num = 1; num <= 5; num++) {
            radioHtml += `<label style="margin-right:15px; cursor:pointer; display:flex; align-items:center;"><input type="radio" name="q${i}" value="${num}" style="margin-right:5px;"> ${num}</label>`;
        }
        row.innerHTML = radioHtml;
        container.appendChild(row);
    }
};

window.openEvalModal = () => {
    document.getElementById('targetGrade').value = "";
    document.getElementById('timeLimitSelect').value = "0";
    document.getElementById('newSubject').value = "";
    const aiBox = document.getElementById('aiKeywordText');
    if(aiBox) aiBox.value = "";
    window.setQCount?.(5);
    resetUpload();
    document.getElementById('evalModal').style.display = 'flex';
};
window.closeEvalModal = () => {
    document.getElementById('evalModal').style.display = 'none';
    resetUpload();
};

// 🌟 시험 저장 (정답 + 키워드). 반 배포는 deploymentModal에서 별도 관리.
const saveBtn = document.getElementById("saveBtn");
if (saveBtn) {
    saveBtn.onclick = async () => {
        const targetGrade = document.getElementById("targetGrade").value;
        const timeLimit = parseInt(document.getElementById("timeLimitSelect").value);
        const subject = document.getElementById("newSubject").value;
        const aiBox = document.getElementById("aiKeywordText");
        const aiText = aiBox ? aiBox.value : "";

        if (!targetGrade) return alert("대상 학년을 선택해주세요!");
        if (!subject) return alert("과목명을 입력해주세요!");
        if (!currentFilesToUpload.length) return alert("⚠️ 먼저 시험지 파일을 업로드해 주세요!");

        const radarKeywords = window.extractKeywordsFromText(aiText);
        const answers = {};
        const qCount = parseInt(document.getElementById("qCountSelect").value);
        for (let i = 1; i <= qCount; i++) {
            const selected = document.querySelector(`input[name="q${i}"]:checked`);
            if (!selected) return alert(`${i}번 문항의 정답을 선택해주세요!`);
            answers[i] = selected.value;
        }

        saveBtn.innerText = "저장 중..."; saveBtn.disabled = true;

        try {
            const ts = Date.now();
            const firstFile = currentFilesToUpload[0];
            const isPdf = firstFile.type === 'application/pdf' ||
                          firstFile.name.toLowerCase().endsWith('.pdf');

            let testData;
            if (isPdf) {
                const fileRef = sRef(storage, `tests/${ts}_${firstFile.name}`);
                await uploadBytes(fileRef, firstFile);
                const imageUrl = await getDownloadURL(fileRef);
                testData = {
                    createdAt: ts, subject, answers, imageUrl, fileType: 'pdf',
                    grade: targetGrade, timeLimit,
                    teacherId: auth.currentUser?.uid || ''
                };
            } else {
                const imageUrls = await Promise.all(
                    currentFilesToUpload.map(async (file, i) => {
                        const fileRef = sRef(storage, `tests/${ts}_${i}_${file.name}`);
                        await uploadBytes(fileRef, file);
                        return getDownloadURL(fileRef);
                    })
                );
                testData = {
                    createdAt: ts, subject, answers, imageUrls,
                    grade: targetGrade, timeLimit,
                    teacherId: auth.currentUser?.uid || ''
                };
            }
            if (radarKeywords) testData.radarKeywords = radarKeywords;

            await addDoc(collection(db, "tests"), testData);
            alert(`[${subject}] 시험이 저장되었습니다.\n시험 목록에서 [배포 관리] 버튼으로 반별 배포를 시작하세요.`);
            window.closeEvalModal();
        } catch (e) { alert("저장 오류: " + e.message); }
        finally { saveBtn.innerText = "AI 분석 및 정답 저장"; saveBtn.disabled = false; }
    };
}

// =====================================================================
// 📡 반별 배포 관리 (deployments 컬렉션)
// =====================================================================
// 배포 문서 ID 규칙: {testId}_{grade}_{class}  → 반당 1개 문서로 관리
const deploymentDocId = (testId, grade, cls) => `${testId}_${grade}_${cls}`;

const generateShortCode = () => Math.random().toString(36).substring(2, 8).toUpperCase();

// 현재 열려 있는 배포 모달의 컨텍스트 — 문자열을 onclick에 직접 삽입하지 않기 위해 사용
let _depCtx = { testId: '', subject: '', grade: '' };

window.openDeploymentModal = async (testId, subject, grade) => {
    _depCtx = { testId, subject, grade };
    document.getElementById('deploymentModalTitle').innerText = subject;
    document.getElementById('deploymentModalGrade').innerText = `${grade}학년 전체 반 배포 관리`;
    document.getElementById('deploymentModal').style.display = 'flex';
    await renderDeploymentRows();
};

const getClassesForGrade = (grade) => {
    return Array.from(new Set(
        rosterAllStudents
            .filter(s => String(s.grade) === String(grade) && s.class != null)
            .map(s => String(s.class))
    )).sort((a, b) => parseInt(a) - parseInt(b));
};

const renderDeploymentRows = async () => {
    const { testId, subject, grade } = _depCtx;
    const container = document.getElementById('deploymentClassRows');
    container.innerHTML = '<div style="padding:14px;text-align:center;color:#999;">로딩 중...</div>';

    let depMap = {};
    try {
        const snap = await getDocs(query(collection(db, "deployments"), where("testId", "==", testId)));
        snap.forEach(d => { depMap[d.data().class] = { ...d.data(), docId: d.id }; });
    } catch (e) {
        container.innerHTML = `<div style="padding:14px;color:#e74c3c;">데이터 로드 오류: ${e.message}</div>`;
        return;
    }

    const classes = getClassesForGrade(_depCtx.grade);
    if (!classes.length) {
        container.innerHTML = `<div style="padding:16px;text-align:center;color:#999;font-size:13px;">등록된 학생이 없습니다.<br>환경설정 → 교사정보에서 학급 수를 먼저 설정해주세요.</div>`;
        return;
    }
    container.innerHTML = '';
    classes.forEach(cls => {
        const dep = depMap[cls];
        const status = dep ? dep.status : 'none';
        const statusLabel = { active: '배포 중', completed: '완료', none: '미배포' }[status] || '미배포';
        const statusColor = { active: '#2ecc71', completed: '#95a5a6', none: '#bdc3c7' }[status];
        const btnLabel = status === 'active' ? '배포 종료' : '배포 시작';
        const btnBg = status === 'active' ? '#e74c3c' : '#3182F6';

        const row = document.createElement('div');
        row.style.cssText = 'display:flex;align-items:center;gap:10px;padding:12px 16px;border-bottom:1px solid var(--border);flex-wrap:wrap;';

        const nameSpan = document.createElement('span');
        nameSpan.style.cssText = 'font-weight:600;font-size:14px;min-width:80px;';
        nameSpan.innerText = `${grade}학년 ${cls}반`;

        const statusSpan = document.createElement('span');
        statusSpan.style.cssText = `font-size:12px;font-weight:600;color:${statusColor};`;
        statusSpan.innerText = statusLabel;

        const btn = document.createElement('button');
        btn.style.cssText = `background:${btnBg};color:#fff;border:none;border-radius:6px;padding:6px 14px;font-size:12px;cursor:pointer;font-weight:600;margin-left:auto;`;
        btn.innerText = btnLabel;
        btn.onclick = () => window.toggleDeployment(cls);

        row.appendChild(nameSpan);
        row.appendChild(statusSpan);

        row.appendChild(btn);
        container.appendChild(row);
    });

    const rows = container.querySelectorAll('div');
    if (rows.length) rows[rows.length - 1].style.borderBottom = 'none';
};

window.toggleDeployment = async (cls) => {
    const { testId, subject, grade } = _depCtx;
    console.log("[toggleDeployment] ctx:", { testId, subject, grade, cls });
    if (!testId || !grade) {
        alert(`배포 컨텍스트 오류: testId="${testId}", grade="${grade}"`);
        return;
    }

    const docRef = doc(db, "deployments", deploymentDocId(testId, grade, cls));
    console.log("[toggleDeployment] docRef path:", docRef.path);

    try {
        const snap = await getDoc(docRef);
        const currentStatus = snap.exists() ? snap.data().status : 'none';
        console.log("[toggleDeployment] currentStatus:", currentStatus);

        if (currentStatus === 'active') {
            await setDoc(docRef, { status: 'completed' }, { merge: true });
        } else {
            // 기존 단축 코드 재사용 또는 새로 생성
            let shortCode = snap.exists() ? snap.data().shortCode : null;
            if (!shortCode) {
                shortCode = generateShortCode();
                const collision = await getDoc(doc(db, "short_links", shortCode));
                if (collision.exists()) shortCode = generateShortCode();
                await setDoc(doc(db, "short_links", shortCode), {
                    grade, class: cls, gradeClass: `${grade}_${cls}`, testId,
                    createdAt: Date.now()
                });
            }

            // 배포 시작 (다른 시험이 이미 active여도 그대로 유지 — 동시 배포 허용)
            await setDoc(docRef, {
                testId, subject, grade,
                class: cls,
                gradeClass: `${grade}_${cls}`,
                status: 'active',
                activatedAt: Date.now(),
                shortCode,
                teacherId: auth.currentUser?.uid || '',
                createdAt: snap.exists() ? snap.data().createdAt : Date.now()
            });
        }
        await renderDeploymentRows();
    } catch (e) {
        alert(`배포 처리 중 오류가 발생했습니다:\n${e.message}`);
    }
};

window.enterClass = (p, name) => {
    const titleElement = document.getElementById('currentClassName');
    if (titleElement) {
        titleElement.innerText = name; 
    }

    const tbody = document.getElementById('studentListBody');
    if (!tbody) return;

    const match = name.match(/(\d+)학년\s*(\d+)반/);
    const targetGrade = match ? match[1] : null;
    const targetClass = match ? match[2] : null;

    onSnapshot(collection(db, "student_scores"), (snapshot) => {
        tbody.innerHTML = ''; 
        window.classStatsData = []; 

        if (snapshot.empty) {
            tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:#999; padding:20px;">아직 제출된 학생 데이터가 없습니다.</td></tr>`;
            return;
        }
        
        let idx = 1; 
        let hasData = false;

        snapshot.forEach((doc) => {
            const data = doc.data();

            if (targetGrade && targetClass) {
                if (String(data.grade) !== String(targetGrade) || String(data.class) !== String(targetClass)) {
                    return; 
                }
            }

            hasData = true;
            window.classStatsData.push(data); 

            const keywordTags = data.keywords ? data.keywords.map(k => `<span class="kw-tag" style="background:#e8f0fe; color:#1a73e8; padding:3px 8px; border-radius:12px; font-size:11px; margin-right:4px;">${k}</span>`).join('') : '-';
            
            let scoreDisplay = "";
            let actionButtons = "";

            if (data.score === "부정행위") {
                scoreDisplay = `<span style="background:#c0392b; color:#fff; padding: 4px 8px; border-radius: 4px; font-weight:bold;">🚨 부정행위 적발</span>`;
                actionButtons = `
                    <button class="view-btn" style="background:#7f8c8d; color:white; border:none; font-size: 11px; padding: 4px 8px; border-radius: 4px; cursor:not-allowed;" disabled>분석 불가 (무효)</button>
                `;
            } else {
                const scoreColor = data.score >= 80 ? '#2ecc71' : '#f39c12';
                scoreDisplay = `<span style="background:${scoreColor}; color:#fff; padding: 4px 8px; border-radius: 4px; font-weight:bold;">${data.score}점</span>`;
                
                actionButtons = `
                    <button class="view-btn btn-wrong" style="background: #e74c3c; color: white; border: none; font-size: 11px; padding: 4px 8px; border-radius: 4px;" onclick="window.sendAIClinic('${data.name}', ${data.score})">오답 전송</button>
                    <button class="view-btn" style="background: #1a73e8; color: white; border: none; font-size: 11px; padding: 4px 8px; border-radius: 4px;" onclick="openStudentInfoModal('${data.name}', ${data.score}, '${data.subject || "일반 평가"}')">상세분석</button>
                `;
            }
            
            const tr = `
                <tr>
                    <td>${idx++}</td>
                    <td><strong>${data.name}</strong></td>
                    <td>${scoreDisplay}</td>
                    <td>${keywordTags}</td>
                    <td>
                        <div style="display: flex; gap: 4px; flex-wrap: wrap;">
                            ${actionButtons}
                        </div>
                    </td>
                </tr>
            `;
            tbody.insertAdjacentHTML('beforeend', tr);
        });

        if (!hasData) {
            tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:#999; padding:20px;">해당 학급(${name})에 아직 제출된 데이터가 없습니다.</td></tr>`;
        }
    });
};

document.getElementById("logoutBtn").onclick = () => signOut(auth).then(() => location.href = "/frontend/login.html");
// =====================================================================
// 📋 시험 목록 → 학생 목록 2단계 OMR 뷰
// =====================================================================
let currentExamUnsubscribe = null; // 이전 학생 목록 실시간 리스너 해제용
let selectedExamDoc = null;        // 현재 선택된 시험 문서

window.showExamList = () => {
    document.getElementById('examListPanel').style.display = '';
    document.getElementById('studentListPanel').style.display = 'none';
    document.getElementById('backToExamListBtn').style.display = 'none';
    document.getElementById('r-title').innerText = '디지털 OMR 시험 목록';
    // 시험 목록: 새 평가 등록만 표시
    document.getElementById('btnNewEval').style.display = '';
    document.getElementById('btnClassStats').style.display = 'none';
    // 드롭다운 초기화
    const label = document.getElementById('classDropdownLabel');
    if (label) label.innerText = '반 선택';
    const menu = document.getElementById('classDropdownMenu');
    if (menu) menu.style.display = 'none';
    selectedExamDoc = null;
    selectedClassFilter = null;
    if (currentExamUnsubscribe) { currentExamUnsubscribe(); currentExamUnsubscribe = null; }
};

window.selectExam = (examId, subject, grade, cls, createdAt) => {
    selectedExamDoc = { id: examId, subject, grade, createdAt };
    document.getElementById('examListPanel').style.display = 'none';
    document.getElementById('studentListPanel').style.display = '';
    document.getElementById('backToExamListBtn').style.display = '';
    document.getElementById('r-title').innerText = '응시 학생 목록';
    document.getElementById('selectedExamTitle').innerText = subject;
    const dateStr = createdAt ? new Date(parseInt(createdAt)).toLocaleDateString('ko-KR') : '';
    document.getElementById('selectedExamMeta').innerText = `${grade}학년  ·  ${dateStr}`;
    // 응시 학생 목록: 학급 오답률 분석만 표시
    document.getElementById('btnNewEval').style.display = 'none';
    document.getElementById('btnClassStats').style.display = '';
    loadStudentsForExam(examId, subject, grade);
};

window.clearCheatingFlag = async (studentName, grade, cls) => {
    if (!confirm(`${studentName} 학생의 재응시를 허용하시겠습니까?`)) return;
    try {
        // 1. student_scores 부정행위 기록 삭제 (있는 경우에만)
        const q = query(
            collection(db, 'student_scores'),
            where('name', '==', studentName),
            where('score', '==', '부정행위')
        );
        const snap = await getDocs(q);
        const targets = snap.docs.filter(d => {
            if (!selectedExamDoc) return true;
            const data = d.data();
            return (data.testId && data.testId === selectedExamDoc.id) ||
                   (!data.testId && data.subject === selectedExamDoc.subject);
        });
        for (const d of targets) await deleteDoc(doc(db, 'student_scores', d.id));

        // 2. exam_sessions 세션 삭제 (항상 실행 — 응시 중/부정행위 모두 처리)
        // 세션 ID는 학생 웹에서 `학년_반_이름_번호_testId` 형식(번호 포함)으로 생성되므로
        // 교사 측에서 ID를 직접 조합하면 번호가 어긋나 삭제에 실패한다(재응시 허용 미동작).
        // → 필드(name + testId)로 조회해 해당 학생의 세션을 모두 삭제 (번호/형식 무관, 견고).
        if (selectedExamDoc) {
            const sessSnap = await getDocs(query(
                collection(db, 'exam_sessions'),
                where('name', '==', studentName),
                where('testId', '==', selectedExamDoc.id)
            ));
            const sessTargets = sessSnap.docs.filter(d => {
                const s = d.data();
                return (!grade || String(s.grade) === String(grade)) &&
                       (!cls   || String(s.class) === String(cls));
            });
            for (const d of sessTargets) await deleteDoc(doc(db, 'exam_sessions', d.id));
        }

        alert(`${studentName} 학생의 재응시가 허용되었습니다.`);
        window.refreshStudentList();
    } catch (e) {
        alert('처리 중 오류가 발생했습니다: ' + e.message);
    }
};

window.refreshStudentList = async () => {
    if (!selectedExamDoc) return;
    const { id, subject, grade } = selectedExamDoc;
    await loadStudentsForExam(id, subject, grade);
};

let selectedClassFilter = null; // null = 전체, "1" ~ "10" = 특정 반

// 드롭다운 열기/닫기
window.toggleClassDropdown = () => {
    const menu = document.getElementById('classDropdownMenu');
    const arrow = document.getElementById('classDropdownArrow');
    const isOpen = menu.style.display !== 'none';
    menu.style.display = isOpen ? 'none' : 'block';
    arrow.style.transform = isOpen ? '' : 'rotate(180deg)';
};

// 드롭다운 바깥 클릭 시 닫기
document.addEventListener('click', (e) => {
    const wrap = document.getElementById('classDropdownWrap');
    if (wrap && !wrap.contains(e.target)) {
        document.getElementById('classDropdownMenu').style.display = 'none';
        document.getElementById('classDropdownArrow').style.transform = '';
    }
});

// 반 드롭다운 목록 렌더링
// deployedClasses: deployments에서 가져온 배포 반 Set
// allStudents: 실제 제출 학생 배열 (카운트용)
const renderClassDropdown = (allStudents, deployedClasses) => {
    const list = document.getElementById('classDropdownList');
    if (!list) return;

    // 배포된 반 + 제출된 반의 합집합 (오름차순)
    const classSet = new Set(deployedClasses);
    allStudents.forEach(s => { if (s.class != null) classSet.add(String(s.class)); });
    const classes = [...classSet].sort((a, b) => parseInt(a) - parseInt(b));

    const grade = selectedExamDoc?.grade || '';
    list.innerHTML = '';

    if (!classes.length) {
        const empty = document.createElement('div');
        empty.style.cssText = 'padding:14px 16px; font-size:13px; color:var(--text-5);';
        empty.innerText = '배포된 반이 없습니다.';
        list.appendChild(empty);
        return;
    }

    classes.forEach(cls => {
        const cnt = allStudents.filter(s => String(s.class) === cls).length;
        const isActive = selectedClassFilter === cls;

        const item = document.createElement('div');
        item.style.cssText = [
            'display:flex', 'align-items:center', 'justify-content:space-between',
            'padding:11px 16px', 'cursor:pointer', 'font-size:14px',
            `background:${isActive ? '#EBF4FF' : 'transparent'}`,
            `color:${isActive ? 'var(--primary)' : 'var(--text-2)'}`,
            `font-weight:${isActive ? '700' : '400'}`,
            'transition:background 0.1s',
        ].join(';');
        item.onmouseenter = () => { if (!isActive) item.style.background = 'var(--surface-2)'; };
        item.onmouseleave = () => { if (!isActive) item.style.background = 'transparent'; };

        const nameSpan = document.createElement('span');
        nameSpan.innerText = `${grade}학년 ${cls}반`;

        const countSpan = document.createElement('span');
        countSpan.style.cssText = 'font-size:12px; color:var(--text-4); font-weight:400;';
        countSpan.innerText = cnt > 0 ? `${cnt}명` : '미제출';

        item.appendChild(nameSpan);
        item.appendChild(countSpan);

        item.onclick = () => {
            selectedClassFilter = cls;
            document.getElementById('classDropdownLabel').innerText = `${grade}학년 ${cls}반`;
            document.getElementById('classDropdownMenu').style.display = 'none';
            document.getElementById('classDropdownArrow').style.transform = '';
            renderClassDropdown(allStudents, deployedClasses);
            renderStudentRows(allStudents.filter(s => String(s.class) === cls));
        };

        list.appendChild(item);
    });
};

// 테이블 행 렌더링 (필터된 배열 기준)
const renderStudentRows = (students) => {
    const tbody = document.getElementById('studentListBody');
    tbody.innerHTML = '';

    if (!students.length) {
        tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;color:#999;padding:20px;">해당 반에 제출된 학생이 없습니다.</td></tr>`;
        return;
    }

    // 반 → 번호 순으로 정렬 (번호 없는 학생은 뒤에 이름순)
    const sorted = [...students].sort((a, b) => {
        const ca = parseInt(a.class) || 0;
        const cb = parseInt(b.class) || 0;
        if (ca !== cb) return ca - cb;
        const na = Number(a.number) || 0;
        const nb = Number(b.number) || 0;
        if (na && nb) return na - nb;
        if (na) return -1;
        if (nb) return 1;
        return (a.name || '').localeCompare(b.name || '', 'ko');
    });

    sorted.forEach((data, i) => {
        const keywordTags = data.keywords
            ? data.keywords.map(k => `<span class="kw-tag" style="background:#e8f0fe;color:#1a73e8;padding:3px 8px;border-radius:12px;font-size:11px;margin-right:4px;">${k}</span>`).join('')
            : '-';

        let scoreDisplay = '', actionButtons = '';
        if (data.score === '응시 중') {
            scoreDisplay = `<span style="background:#2980b9;color:#fff;padding:4px 8px;border-radius:4px;font-weight:bold;">응시 중</span>`;
            actionButtons = `
                <button class="view-btn" style="background:#e67e22;color:white;border:none;font-size:11px;padding:4px 8px;border-radius:4px;cursor:pointer;" onclick="window.clearCheatingFlag('${data.name}','${data.grade}','${data.class}')">재응시 허용</button>
            `;
        } else if (data.score === '부정행위') {
            scoreDisplay = `<span style="background:#c0392b;color:#fff;padding:4px 8px;border-radius:4px;font-weight:bold;">부정행위 적발</span>`;
            actionButtons = `
                <button class="view-btn" style="background:#7f8c8d;color:white;border:none;font-size:11px;padding:4px 8px;border-radius:4px;cursor:not-allowed;" disabled>분석 불가 (무효)</button>
                <button class="view-btn" style="background:#e67e22;color:white;border:none;font-size:11px;padding:4px 8px;border-radius:4px;cursor:pointer;" onclick="window.clearCheatingFlag('${data.name}','${data.grade}','${data.class}')">재응시 허용</button>
            `;
        } else {
            const scoreColor = data.score >= 80 ? '#2ecc71' : '#f39c12';
            scoreDisplay = `<span style="background:${scoreColor};color:#fff;padding:4px 8px;border-radius:4px;font-weight:bold;">${data.score}점</span>`;
            actionButtons = `
                <button class="view-btn" style="background:#1a73e8;color:white;border:none;font-size:11px;padding:4px 8px;border-radius:4px;" onclick="openStudentInfoModal('${data.name}',${data.score},'${(data.subject||'일반 평가').replace(/'/g,"\\'")}')">상세분석</button>
            `;
        }

        const classBadge = data.class
            ? `<span style="background:#e8f0fe;color:#1a73e8;padding:2px 7px;border-radius:10px;font-size:11px;margin-left:6px;">${data.grade||''}학년 ${data.class}반${data.number ? ' ' + data.number + '번' : ''}</span>`
            : '';

        tbody.insertAdjacentHTML('beforeend', `
            <tr>
                <td>${i + 1}</td>
                <td><strong>${data.name}</strong>${classBadge}</td>
                <td>${scoreDisplay}</td>
                <td>${keywordTags}</td>
                <td><div style="display:flex;gap:4px;flex-wrap:wrap;">${actionButtons}</div></td>
            </tr>
        `);
    });
};

const loadStudentsForExam = async (examId, subject, grade) => {
    if (currentExamUnsubscribe) { currentExamUnsubscribe(); currentExamUnsubscribe = null; }
    selectedClassFilter = null;

    // 드롭다운 버튼 라벨 초기화
    const labelEl = document.getElementById('classDropdownLabel');
    if (labelEl) labelEl.innerText = '반 선택';

    const tbody = document.getElementById('studentListBody');
    tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;color:#999;padding:20px;">데이터 로딩 중...</td></tr>`;
    window.classStatsData = [];

    // 배포된 반 목록 선조회 (제출 여부와 무관하게 드롭다운에 표시)
    let deployedClasses = new Set();
    try {
        const depSnap = await getDocs(query(collection(db, "deployments"), where("testId", "==", examId)));
        depSnap.forEach(d => {
            const cls = d.data().class;
            if (cls != null) deployedClasses.add(String(cls));
        });
    } catch (e) {
        console.warn("deployments 조회 실패:", e.message);
    }

    // 배포된 반이 있으면 첫 번째 반을 자동 선택
    if (deployedClasses.size > 0) {
        const firstClass = [...deployedClasses].sort((a, b) => parseInt(a) - parseInt(b))[0];
        selectedClassFilter = firstClass;
        if (labelEl) labelEl.innerText = `${grade}학년 ${firstClass}반`;
    }

    // 학생 제출 데이터 실시간 감지
    currentExamUnsubscribe = onSnapshot(collection(db, "student_scores"), async (snapshot) => {
        window.classStatsData = [];

        snapshot.forEach((docSnap) => {
            const data = docSnap.data();
            const matchByTestId = data.testId && data.testId === examId;
            const matchBySubject = !data.testId
                && data.subject === subject
                && String(data.grade) === String(grade);
            if (!matchByTestId && !matchBySubject) return;
            window.classStatsData.push(data);
        });

        // exam_sessions 'active' 학생 추가 (입장했지만 미제출/미차단 학생)
        try {
            const sessionSnap = await getDocs(query(
                collection(db, 'exam_sessions'),
                where('testId', '==', examId),
                where('status', '==', 'active')
            ));
            sessionSnap.forEach(d => {
                const s = d.data();
                const alreadyIn = window.classStatsData.some(
                    r => r.name === s.name && String(r.grade) === String(s.grade) && String(r.class) === String(s.class)
                );
                if (!alreadyIn) {
                    window.classStatsData.push({ ...s, score: '응시 중', keywords: [] });
                }
            });
        } catch(e) { console.warn('exam_sessions 조회 실패:', e.message); }

        // 드롭다운 목록 갱신
        renderClassDropdown(window.classStatsData, deployedClasses);

        // 테이블: 선택된 반 필터 적용
        const toShow = selectedClassFilter === null
            ? window.classStatsData
            : window.classStatsData.filter(s => String(s.class) === selectedClassFilter);

        renderStudentRows(toShow);
    });
};

let deploymentStatusMap = {};

const updateDeployDots = () => {
    Object.entries(deploymentStatusMap).forEach(([testId, isActive]) => {
        const dot = document.getElementById(`deploy-dot-${testId}`);
        if (dot) dot.style.background = isActive ? '#38a169' : '#e53e3e';
    });
};

const renderExamList = () => {
    const grid = document.getElementById('examListGrid');
    if (!grid) return;

    // 배포 상태 실시간 감지 → 상태 점 업데이트
    const uid = auth.currentUser?.uid;
    onSnapshot(query(collection(db, 'deployments'), where('teacherId', '==', uid)), (depSnap) => {
        deploymentStatusMap = {};
        depSnap.forEach(d => {
            const data = d.data();
            if (!data.testId) return;
            if (data.status === 'active') {
                deploymentStatusMap[data.testId] = true;
            } else if (deploymentStatusMap[data.testId] === undefined) {
                deploymentStatusMap[data.testId] = false;
            }
        });
        updateDeployDots();
    });

    onSnapshot(query(collection(db, "tests"), where('teacherId', '==', uid), orderBy("createdAt", "desc")), (snapshot) => {
        grid.innerHTML = '';
        analysisExamsList = [];
        if (snapshot.empty) {
            grid.innerHTML = `<div style="grid-column:1/-1;text-align:center;color:var(--text-5);padding:60px;">등록된 시험이 없습니다.</div>`;
            populateAnalysisDropdown([]);
            return;
        }
        snapshot.forEach(docSnap => {
            const d = docSnap.data();
            const id = docSnap.id;
            const subject = d.subject || '(제목 없음)';
            const grade = d.grade || '';
            const dateStr = d.createdAt ? new Date(d.createdAt).toLocaleDateString('ko-KR') : '-';
            const timeLimitTxt = d.timeLimit > 0 ? `${d.timeLimit}분 제한` : '무제한';
            analysisExamsList.push({ id, subject, grade, createdAt: d.createdAt || 0 });

            const card = document.createElement('div');
            card.style.cssText = 'background:var(--surface);border:1px solid var(--border);border-radius:12px;padding:20px;transition:box-shadow 0.15s,border-color 0.15s;';
            card.onmouseenter = () => { card.style.boxShadow = '0 4px 16px rgba(0,0,0,0.10)'; card.style.borderColor = 'var(--primary)'; };
            card.onmouseleave = () => { card.style.boxShadow = ''; card.style.borderColor = 'var(--border)'; };
            card.innerHTML = `
                <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:8px;margin-bottom:8px;">
                    <div style="display:flex;align-items:center;gap:8px;min-width:0;">
                        <span id="deploy-dot-${id}" style="display:inline-block;width:9px;height:9px;border-radius:50%;background:#e53e3e;flex-shrink:0;transition:background 0.3s;" title="배포 상태"></span>
                        <span style="font-size:16px;font-weight:700;color:var(--text-1);word-break:break-word;">${subject}</span>
                    </div>
                    <div style="display:flex;gap:4px;flex-shrink:0;">
                        <button data-action="edit" style="width:28px;height:28px;border:1px solid var(--border);border-radius:6px;background:var(--surface);cursor:pointer;font-size:14px;color:var(--text-3);display:flex;align-items:center;justify-content:center;transition:background 0.15s;" title="수정" onmouseenter="this.style.background='var(--surface-2)'" onmouseleave="this.style.background='var(--surface)'">✎</button>
                        <button data-action="delete" style="width:28px;height:28px;border:1px solid #fed7d7;border-radius:6px;background:#fff5f5;cursor:pointer;font-size:13px;font-weight:700;color:#e53e3e;display:flex;align-items:center;justify-content:center;transition:background 0.15s;" title="삭제" onmouseenter="this.style.background='#fed7d7'" onmouseleave="this.style.background='#fff5f5'">✕</button>
                    </div>
                </div>
                <div style="font-size:13px;color:var(--text-4);margin-bottom:4px;">${dateStr} &nbsp;·&nbsp; ${timeLimitTxt}</div>
                <div style="font-size:13px;color:var(--text-4);margin-bottom:12px;">${grade || '-'}학년</div>
                <div style="font-size:12px;background:var(--surface-2);border-radius:6px;padding:6px 10px;color:var(--text-3);margin-bottom:12px;">문항 수: ${Object.keys(d.answers || {}).length}문항</div>
                <div style="display:flex;gap:8px;flex-wrap:wrap;">
                    <button data-action="deploy" style="flex:1;background:#f39c12;color:#fff;border:none;border-radius:6px;padding:7px 0;font-size:12px;font-weight:600;cursor:pointer;">배포 관리</button>
                    <button data-action="students" style="flex:1;background:var(--primary);color:#fff;border:none;border-radius:6px;padding:7px 0;font-size:12px;font-weight:600;cursor:pointer;">응시자 보기</button>
                    <button data-action="analysis" style="flex:1;background:#6c3fc5;color:#fff;border:none;border-radius:6px;padding:7px 0;font-size:12px;font-weight:600;cursor:pointer;">학습 분석</button>
                </div>
            `;

            // 이벤트 리스너를 JS로 연결 — 과목명 등 특수문자에 안전
            card.querySelector('[data-action="deploy"]').addEventListener('click', (e) => {
                e.stopPropagation();
                window.openDeploymentModal(id, subject, grade);
            });
            card.querySelector('[data-action="students"]').addEventListener('click', (e) => {
                e.stopPropagation();
                selectExam(id, subject, grade, '', d.createdAt || '');
            });
            card.querySelector('[data-action="analysis"]').addEventListener('click', (e) => {
                e.stopPropagation();
                window.openAnalysisForExam(id);
            });
            card.querySelector('[data-action="edit"]').addEventListener('click', (e) => {
                e.stopPropagation();
                window.openEditExamModal(id);
            });
            card.querySelector('[data-action="delete"]').addEventListener('click', (e) => {
                e.stopPropagation();
                window.deleteExam(id, subject);
            });

            grid.appendChild(card);
        });
        populateAnalysisDropdown(analysisExamsList);
        setTimeout(updateDeployDots, 0); // 카드 DOM 생성 직후 현재 배포 상태 반영
    });
};

const renderActiveDeployList = (docs) => {
    const el = document.getElementById('activeDeployList');
    if (!el) return;

    if (!docs.length) {
        el.innerHTML = `<div class="upcoming-list"><div style="color:var(--text-5);font-size:13px;text-align:center;padding:4px 0;">현재 배포 중인 시험이 없습니다.</div></div>`;
        return;
    }

    // testId 기준으로 묶기
    const grouped = {};
    docs.forEach(d => {
        if (!grouped[d.testId]) grouped[d.testId] = { subject: d.subject || '(제목 없음)', grade: d.grade || '', classes: [] };
        if (d.class != null) grouped[d.testId].classes.push(String(d.class));
    });

    const entries = Object.values(grouped);
    el.innerHTML = `<div class="upcoming-list">` + entries.map(g => {
        const classList = g.classes
            .sort((a, b) => parseInt(a) - parseInt(b))
            .map(c => `${c}반`).join(', ');
        return `
            <div class="upcoming-item">
                <div class="upcoming-name">
                    <span class="dot dot-green"></span>${g.subject}
                </div>
                <div class="upcoming-detail">${g.grade}학년 &nbsp;·&nbsp; ${classList}</div>
            </div>`;
    }).join('') + `</div>`;
};

// 페이지 첫 로드 시 정적 HTML 데이터로 초기화 및 렌더링
document.addEventListener('DOMContentLoaded', () => {
    // 정적 HTML의 고정 일정 항목으로 fixedScheduleData 초기화 (Firebase 로드 전 기본값)
    window.fixedScheduleData = [...document.querySelectorAll('.fixed-schedule-card .fixed-item')].map(item => ({
        title:    item.querySelector('.fixed-title')?.textContent?.trim()    || '',
        detail:   item.querySelector('.fixed-detail')?.textContent?.trim()   || '',
        location: item.querySelector('.fixed-location')?.textContent?.trim() || '',
    })).filter(d => d.title);
    renderNextClass();
    renderUpcomingSchedule();
});

onAuthStateChanged(auth, (user) => {
    if (!user) { location.href = "/frontend/login.html"; return; }
    loadAndApplyPrefs(user.uid);
    renderExamList();

    // 배포 중인 시험 실시간 감지
    onSnapshot(query(collection(db, 'deployments'), where('teacherId', '==', user.uid), where('status', '==', 'active')), (snap) => {
        renderActiveDeployList(snap.docs.map(d => d.data()));
    });

    // 🌟 등록된 시험들의 키워드 정보를 맵핑해두는 로직
    onSnapshot(query(collection(db, "tests"), where('teacherId', '==', user.uid)), (snapshot) => {
        window.testKeywordsMap = {};
        snapshot.forEach(doc => {
            const data = doc.data();
            if (data.subject && data.radarKeywords) {
                window.testKeywordsMap[data.subject] = data.radarKeywords;
            }
        });
    });
    
    onSnapshot(collection(legacyDb, "users"), (snapshot) => {
        rosterFromUsers.clear();
        snapshot.forEach(doc => {
            const d = doc.data();
            if (!d.childGrade || !d.childClass || !d.childName) return;
            const num = d.childNumber || 0;
            const key = num
                ? `${d.childGrade}_${d.childClass}_${num}_${d.childName}`
                : `${d.childGrade}_${d.childClass}_${d.childName}`;
            rosterFromUsers.set(key, {
                name:   d.childName,
                grade:  String(d.childGrade),
                class:  String(d.childClass),
                number: num,
                school: d.childSchool || '',
                email:  d.email || '',
            });
        });
        mergeRosterData();
    });

    onSnapshot(collection(db, "students"), (snapshot) => {
        rosterFromStudents.clear();
        snapshot.forEach(doc => {
            const d = doc.data();
            const num = d.number || 0;
            const key = num
                ? `${d.grade}_${d.class}_${num}_${d.name}`
                : `${d.grade}_${d.class}_${d.name}`;
            rosterFromStudents.set(key, d);
        });
        mergeRosterData();
    });

    onSnapshot(collection(db, "student_scores"), (snapshot) => {
        rosterFromScores.clear();
        snapshot.forEach(doc => {
            const d = doc.data();
            const num = d.number || 0;
            const key = num
                ? `${d.grade}_${d.class}_${num}_${d.name}`
                : `${d.grade}_${d.class}_${d.name}`;
            if (!rosterFromScores.has(key) || (d.timestamp > (rosterFromScores.get(key).timestamp || 0))) {
                const status = d.score === '부정행위' ? '부정행위' : '제출 완료';
                rosterFromScores.set(key, { ...d, status });
            }
        });
        mergeRosterData();
    });
});

const dropZone    = document.getElementById('drop-zone');
const fileInput   = document.getElementById('file-input');
const previewList = document.getElementById('image-preview-list');
const changeFileArea = document.getElementById('change-file-area');
const changeFileBtn  = document.getElementById('change-file-btn');

const resetUpload = () => {
    currentFilesToUpload = [];
    dropZone.style.display = '';
    previewList.style.display = 'none';
    previewList.innerHTML = '';
    changeFileArea.style.display = 'none';
    fileInput.value = '';
};

const renderUploadPreview = () => {
    if (!currentFilesToUpload.length) { resetUpload(); return; }

    const isPdf = currentFilesToUpload[0].type === 'application/pdf' ||
                  currentFilesToUpload[0].name.toLowerCase().endsWith('.pdf');

    dropZone.style.display = 'none';
    previewList.style.display = 'block';
    previewList.innerHTML = '';

    if (isPdf) {
        previewList.innerHTML = `
            <div style="display:flex;align-items:center;justify-content:space-between;background:#f7f8fa;border:1px solid #e2e8f0;border-radius:8px;padding:12px 14px;margin-bottom:8px;">
                <span style="font-size:14px;color:#4a5568;">📄 ${currentFilesToUpload[0].name}</span>
                <button type="button" onclick="window.removeUploadFile(0)" style="background:none;border:none;font-size:16px;color:#a0aec0;cursor:pointer;">✕</button>
            </div>`;
        changeFileArea.style.display = 'none';
    } else {
        const grid = document.createElement('div');
        grid.style.cssText = 'display:flex;flex-wrap:wrap;gap:8px;margin-bottom:8px;';
        currentFilesToUpload.forEach((file, idx) => {
            const wrapper = document.createElement('div');
            wrapper.style.cssText = 'position:relative;width:80px;height:80px;border-radius:8px;overflow:hidden;border:1px solid #e2e8f0;';
            const img = document.createElement('img');
            img.style.cssText = 'width:100%;height:100%;object-fit:cover;';
            const removeBtn = document.createElement('button');
            removeBtn.type = 'button';
            removeBtn.textContent = '✕';
            removeBtn.style.cssText = 'position:absolute;top:2px;right:2px;background:rgba(0,0,0,0.55);color:#fff;border:none;border-radius:50%;width:20px;height:20px;font-size:11px;cursor:pointer;display:flex;align-items:center;justify-content:center;padding:0;';
            removeBtn.onclick = () => window.removeUploadFile(idx);
            const num = document.createElement('span');
            num.textContent = idx + 1;
            num.style.cssText = 'position:absolute;bottom:2px;left:4px;font-size:11px;font-weight:700;color:#fff;text-shadow:0 1px 2px rgba(0,0,0,0.7);';
            const reader = new FileReader();
            reader.onload = e => { img.src = e.target.result; };
            reader.readAsDataURL(file);
            wrapper.append(img, removeBtn, num);
            grid.appendChild(wrapper);
        });
        previewList.appendChild(grid);

        if (currentFilesToUpload.length < 10) {
            changeFileArea.style.display = 'block';
            changeFileBtn.textContent = '➕ 이미지 추가';
        } else {
            changeFileArea.style.display = 'none';
        }
    }
};

window.removeUploadFile = (idx) => {
    currentFilesToUpload.splice(idx, 1);
    renderUploadPreview();
};

if (dropZone && fileInput) {
    dropZone.addEventListener('click', () => fileInput.click());
    changeFileBtn?.addEventListener('click', () => fileInput.click());

    dropZone.addEventListener('dragover', e => { e.preventDefault(); dropZone.style.background = '#eef3ff'; });
    dropZone.addEventListener('dragleave', () => { dropZone.style.background = ''; });
    dropZone.addEventListener('drop', e => {
        e.preventDefault();
        dropZone.style.background = '';
        if (e.dataTransfer.files.length) handleFiles(Array.from(e.dataTransfer.files));
    });

    fileInput.addEventListener('change', function() {
        if (this.files.length > 0) handleFiles(Array.from(this.files));
        this.value = '';
    });
}

function handleFiles(newFiles) {
    const hasPdf = newFiles.some(f => f.type === 'application/pdf' || f.name.toLowerCase().endsWith('.pdf'));
    if (hasPdf) {
        // PDF는 단독 1개만 허용
        const pdf = newFiles.find(f => f.type === 'application/pdf' || f.name.toLowerCase().endsWith('.pdf'));
        currentFilesToUpload = [pdf];
    } else {
        // 이미지: 기존 목록에 추가 (최대 10장)
        const images = newFiles.filter(f => f.type.startsWith('image/'));
        currentFilesToUpload = [...currentFilesToUpload.filter(f => !f.type.startsWith('application/pdf')), ...images].slice(0, 10);
    }
    renderUploadPreview();
}

let classChartInstance = null;
let studentChartInstance = null;
let currentStatsScope = 'class'; // 'class' | 'grade'

// 범위에 맞는 학생 배열 반환
const getStatsStudents = () => {
    const all = window.classStatsData;
    if (currentStatsScope === 'class' && selectedClassFilter) {
        return all.filter(s => String(s.class) === selectedClassFilter);
    }
    return all;
};

// 범위 토글 버튼 UI 업데이트
const updateScopeButtons = () => {
    const isClass = currentStatsScope === 'class';
    const active   = 'background:#3182F6;color:#fff;';
    const inactive = 'background:var(--surface-2);color:var(--text-3);';
    document.getElementById('scopeBtnClass').style.cssText = `padding:6px 16px;font-size:13px;font-weight:600;border:none;cursor:pointer;${isClass ? active : inactive}`;
    document.getElementById('scopeBtnGrade').style.cssText = `padding:6px 16px;font-size:13px;font-weight:600;border:none;cursor:pointer;${isClass ? inactive : active}`;

    const grade = selectedExamDoc?.grade || '';
    const labelEl = document.getElementById('statsScopeLabel');
    if (labelEl) {
        if (isClass && selectedClassFilter) {
            labelEl.innerText = `— ${grade}학년 ${selectedClassFilter}반 학생만`;
        } else if (isClass) {
            labelEl.innerText = '— 현재 조회 중인 학생 전체';
        } else {
            labelEl.innerText = `— ${grade}학년 모든 반 합산`;
        }
    }
};

window.setStatsScope = (scope) => {
    currentStatsScope = scope;
    updateScopeButtons();
    const qNum = parseInt(document.getElementById('statQuestionSelect').value) || 1;
    window.changeQuestionStats(qNum);
};

// 문항 선택 옵션 동적 생성
const buildQuestionSelect = () => {
    const sel = document.getElementById('statQuestionSelect');
    const students = getStatsStudents();
    // 제출된 답안에서 최대 문항 수 추출
    let maxQ = 5;
    students.forEach(s => {
        const ans = s.student_answers || s.answers || {};
        const keys = Object.keys(ans).map(Number).filter(n => !isNaN(n));
        if (keys.length > maxQ) maxQ = Math.max(...keys);
    });
    sel.innerHTML = '';
    for (let i = 1; i <= maxQ; i++) {
        const opt = document.createElement('option');
        opt.value = i; opt.innerText = `${i}번 문항`;
        sel.appendChild(opt);
    }
    sel.value = Math.min(3, maxQ); // 기본 3번 (없으면 1번)
    return parseInt(sel.value);
};

window.openClassStatsModal = () => {
    currentStatsScope = 'class'; // 항상 반별로 시작
    document.getElementById('classStatsModal').style.display = 'flex';
    updateScopeButtons();
    const firstQ = buildQuestionSelect();
    window.changeQuestionStats(firstQ);
};

window.changeQuestionStats = (qNumStr) => {
    const qNum = parseInt(qNumStr);
    const ctx = document.getElementById('classBarChart').getContext('2d');
    if (classChartInstance) classChartInstance.destroy();

    const students = getStatsStudents();
    let totalStudents = students.length;
    let counts = { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 };

    students.forEach(student => {
        const answers = student.student_answers || student.answers;
        if (answers && answers[qNum]) {
            counts[answers[qNum]]++;
        }
    });

    const pct = (n) => totalStudents ? Math.round(counts[n] / totalStudents * 100) : 0;
    const p1 = pct(1), p2 = pct(2), p3 = pct(3), p4 = pct(4), p5 = pct(5);
    const pcts = [p1, p2, p3, p4, p5];

    // 가장 많이 선택된 보기 찾기
    const maxPct = Math.max(...pcts);
    const maxChoice = pcts.indexOf(maxPct) + 1;

    // 동적 인사이트 텍스트
    const insightText = document.getElementById('aiInsightText');
    const scopeStr = currentStatsScope === 'class' && selectedClassFilter
        ? `${selectedExamDoc?.grade || ''}학년 ${selectedClassFilter}반`
        : `${selectedExamDoc?.grade || ''}학년 전체`;

    if (!totalStudents) {
        insightText.innerHTML = `"${scopeStr}에 해당하는 제출 데이터가 없습니다."`;
    } else if (maxPct >= 40) {
        insightText.innerHTML = `"[${scopeStr}] ${qNum}번 문항에서 <strong>${maxChoice}번 마킹</strong>을 선택한 학생이 ${maxPct}%(${counts[maxChoice]}명)로 집중됩니다. 해당 선택지와 관련된 개념을 다음 수업에서 집중 복습할 것을 권장합니다."`;
    } else {
        insightText.innerHTML = `"[${scopeStr}] ${qNum}번 문항은 특이한 오답 쏠림 현상이 발견되지 않았습니다. 현재의 학습 진도를 유지하세요."`;
    }

    // 차트 색상: 가장 많이 선택된 보기를 강조
    const barColors = [1,2,3,4,5].map(n => n === maxChoice && maxPct >= 40 ? '#e74c3c' : '#a0bde8');

    const scopeLabel = currentStatsScope === 'class' && selectedClassFilter
        ? `${selectedExamDoc?.grade || ''}학년 ${selectedClassFilter}반 · ${qNum}번 문항 선택 비율 (%)`
        : `${selectedExamDoc?.grade || ''}학년 전체 · ${qNum}번 문항 선택 비율 (%)`;

    classChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['1번 마킹', '2번 마킹', '3번 마킹', '4번 마킹', '5번 마킹'],
            datasets: [{
                label: scopeLabel,
                data: [p1, p2, p3, p4, p5],
                backgroundColor: barColors,
            }]
        },
        options: { responsive: true, scales: { y: { beginAtZero: true, max: 100 } }, animation: { duration: 600 } }
    });
};

// =====================================================================
// 🌟 2. 상세분석 오픈: 실제 정오답 데이터와 5대 키워드 자동 매칭!
// =====================================================================
window.openStudentInfoModal = async (studentName, studentScore, subjectName) => {
    document.getElementById('generatedSeteukArea').style.display = 'none';
    document.getElementById('studentDynamicText').innerHTML = "서버에서 실제 시험 데이터와 학생 답안을 교차 분석 중...";
    document.getElementById('infoStudentName').innerText = studentName;
    document.getElementById('studentInfoModal').style.display = 'flex';

    const ctx = document.getElementById('studentRadarChart').getContext('2d');
    if (studentChartInstance) studentChartInstance.destroy();

    const base = studentScore || 0;
    let chartData = [];

    // radarKeywords 우선순위: ① student_scores 직접 포함 → ② tests 문서 재조회 → ③ 과목명 쿼리
    let categories = ['개념 이해도', '원리 적용력', '문제 해결력', '추론 및 분석', '자료 해석력'];
    try {
        const studentData = window.classStatsData.find(s => s.name === studentName && s.subject === subjectName);
        if (studentData?.radarKeywords?.length === 5) {
            categories = studentData.radarKeywords;
        } else {
            const testId = studentData && studentData.testId;
            if (testId) {
                const testDocSnap = await getDoc(doc(db, "tests", testId));
                if (testDocSnap.exists() && testDocSnap.data().radarKeywords) {
                    categories = testDocSnap.data().radarKeywords;
                }
            } else {
                const qTest = query(collection(db, "tests"), where("subject", "==", subjectName));
                const testSnap = await getDocs(qTest);
                if (!testSnap.empty) {
                    const testData = testSnap.docs.map(d => d.data()).find(d => d.radarKeywords);
                    if (testData) categories = testData.radarKeywords;
                }
            }
        }
    } catch(e) { /* 키워드 로드 실패 시 기본값 유지 */ }

    let weakIndex = (studentName.length + base) % 5;
    let weakCategory = categories[weakIndex];

    let statusText = "";
    let weaknessText = "";
    let prescriptionText = "";

    if (base === 100) {
        chartData = [100, 100, 100, 100, 100];
        statusText = `<p>📈 <strong>종합 성취도:</strong> <span style="color:#27ae60; font-weight:bold;">100% (최우수)</span></p>`;
        weaknessText = `<p>🎯 <strong>주요 취약점:</strong><br><span style="color: #27ae60; font-weight: bold; font-size: 15px;">발견되지 않음 (전 영역 완벽)</span></p>`;
        prescriptionText = `<p>💡 <strong>AI 처방:</strong><br>모든 네트워크 기초 개념을 완벽하게 숙지했습니다. 심화 응용 문제 풀이 단계로 넘어가셔도 좋습니다.</p>`;
    } else {
        let weakScore = Math.max(0, base - 25);

        for(let i = 0; i < 5; i++) {
            if(i === weakIndex) {
                chartData.push(weakScore);
            } else {
                chartData.push(Math.min(100, base + ((i+1)*5)));
            }
        }

        let grade = base >= 80 ? "양호" : "보완 필요";
        let gradeColor = base >= 80 ? "#1a73e8" : "#e74c3c";

        statusText = `<p><strong>종합 성취도:</strong> <span style="color:${gradeColor}; font-weight:bold;">${base}% (${grade})</span></p>`;
        weaknessText = `<p><strong>주요 취약점:</strong><br><span style="color: #e74c3c; font-weight: bold; font-size: 15px;">${weakCategory}</span></p>`;
        prescriptionText = `<p><strong>AI 처방:</strong><br>전반적 이해도는 점수(${base}점)를 따라가나, 유독 <strong>'${weakCategory}'</strong> 파트의 정답률이 <strong>${weakScore}%</strong>로 낮게 도출되었습니다. 해당 단원 오답 노트를 통한 집중 관리가 강력히 권장됩니다.</p>`;
    }

    document.getElementById('studentDynamicText').innerHTML = statusText + weaknessText + prescriptionText;

    window.currentAnalysisContext = {
        name: studentName,
        score: studentScore,
        weak: weakCategory,
        strong: categories[weakIndex],
        subject: subjectName
    };

    const isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const gridCol    = isDark ? 'rgba(255,255,255,0.40)' : 'rgba(0,0,0,0.30)';
    const tickCol    = isDark ? 'rgba(255,255,255,0.80)' : '#444';
    const labelCol   = isDark ? 'rgba(255,255,255,0.90)' : '#222';
    const backdropCol = 'rgba(0,0,0,0)';

    studentChartInstance = new Chart(ctx, {
        type: 'radar',
        data: {
            labels: categories,
            datasets: [{
                label: `실시간 성취도 (%)`,
                data: chartData,
                backgroundColor: 'rgba(49, 130, 246, 0.2)',
                borderColor: '#3182F6',
                borderWidth: 2,
                pointBackgroundColor: '#1B64DA',
                pointBorderColor: '#fff',
                pointRadius: 4,
            }]
        },
        options: {
            responsive: true,
            scales: {
                r: {
                    suggestedMin: 0,
                    suggestedMax: 100,
                    ticks: {
                        stepSize: 20,
                        color: tickCol,
                        backdropColor: backdropCol,
                        font: { size: 11 },
                    },
                    grid: { color: gridCol },
                    angleLines: { color: gridCol },
                    pointLabels: {
                        color: labelCol,
                        font: { size: 12, weight: '500' },
                    },
                }
            },
            plugins: {
                legend: {
                    labels: { color: labelCol, font: { size: 12 } }
                }
            }
        }
    });
};

window.generateAutoSeteukFromAnalysis = () => {
    const ctx = window.currentAnalysisContext;
    if(!ctx) return;

    let seteukText = "";
    
    if (ctx.score >= 90) {
        seteukText = `[${ctx.subject}] 수업에 주도적으로 참여하며 전 영역에서 <strong>우수한 성취(${ctx.score}점)</strong>를 보임. 특히 <strong>'${ctx.strong}'</strong> 파트에 대한 이해가 깊으며, 이를 바탕으로 심화 문제를 스스로 해결하려는 자기주도적 탐구력이 돋보임.`;
    } else if (ctx.score >= 60) {
        seteukText = `[${ctx.subject}] 학업에 대한 열의가 높으며 <strong>'${ctx.strong}'</strong> 파트에서 양호한 성과를 보임. <strong>'${ctx.weak}'</strong> 개념 연계 문항에서 다소 혼동이 있었으나, 맞춤형 오답 클리닉을 적극 수용하여 약점을 완벽히 보완하는 성실함을 갖춤.`;
    } else {
        seteukText = `[${ctx.subject}] 어려운 개념 앞에서도 포기하지 않는 태도가 훌륭함. 특히 <strong>'${ctx.weak}'</strong> 파트의 기초를 튼튼히 하기 위해 적극적으로 질문하고 자기주도적 오답노트를 작성하며 점진적인 학업 성장을 이루어내고 있음.`;
    }

    const area = document.getElementById('generatedSeteukArea');
    area.innerHTML = `<strong>AI 맞춤형 세특 초안:</strong><br><span style="color:#191F28; margin-top: 5px; display: inline-block;">${seteukText}</span>`;
    area.style.display = 'block';
};

window.pendingClinicContext = null;

window.sendAIClinic = (studentName, score) => {
    window.openAIClinicModal(studentName, score);
};

window.openAIClinicModal = (studentName, score) => {
    const label = document.getElementById('clinicMessage');
    const list = document.getElementById('clinicQuestions');
    const btn  = document.getElementById('clinicSendBtn');
    if (label) label.innerHTML = `현재 <strong>${studentName}</strong> 학생의 취약점을 바탕으로 AI가 생성한 <strong>유사 문제 3개</strong>를 전송합니다.`;
    const questions = window.generateSimilarQuestions(studentName);
    if (list) list.innerHTML = `<ol style="padding-left:18px; margin:0;">${questions.map(q => `<li style="margin-bottom:8px; line-height:1.6;">${q}</li>`).join('')}</ol>`;
    if (btn) {
        btn.disabled = false;
        btn.innerText = '전송';
    }

    window.pendingClinicContext = {
        name: studentName,
        score,
        questions,
        timestamp: Date.now()
    };
    document.getElementById('aiClinicModal').style.display = 'flex';
};

window.generateSimilarQuestions = (studentName) => {
    const base = studentName || '학생';
    return [
        `${base}의 오답 패턴을 보완하는 AI 쌍둥이 문제 1번: 핵심 개념 적용 문제입니다.`,
        `${base}의 취약 개념을 강화하는 AI 쌍둥이 문제 2번: 상황 제시형 응용 문제입니다.`,
        `${base}의 이해도를 점검하는 AI 쌍둥이 문제 3번: 선택지 분석형 문제입니다.`
    ];
};

window.confirmSendClinic = async () => {
    const ctx = window.pendingClinicContext;
    if (!ctx) return alert('전송할 클리닉 정보가 없습니다.');
    const btn = document.getElementById('clinicSendBtn');
    if (btn) { btn.disabled = true; btn.innerText = '전송 중...'; }

    try {
        await addDoc(collection(db, 'clinic_notes'), {
            name: ctx.name,
            score: ctx.score,
            questions: ctx.questions,
            type: 'AI쌍둥이문제',
            timestamp: ctx.timestamp,
        });
        alert(`✅ ${ctx.name} 학생에게 AI 쌍둥이 문제가 전송되었습니다!`);
        window.closeModal('aiClinicModal');
    } catch (e) {
        console.error('오답 클리닉 전송 오류:', e);
        alert('전송 중 오류가 발생했습니다.');
    } finally {
        if (btn) { btn.disabled = false; btn.innerText = '전송'; }
        window.pendingClinicContext = null;
    }
};

// =====================================================================
// 실시간 KST 일정 동기화 및 기타 유틸
// =====================================================================

const getKST = () => new Date(new Date().toLocaleString('en-US', { timeZone: 'Asia/Seoul' }));

const toMinutes = (hhmm) => {
    const [h, m] = hhmm.split(':').map(Number);
    return h * 60 + m;
};

const CLASS_SCHEDULE = [
    { day: 1, period: 1, start: '09:00', end: '09:50', subject: '수학', grade: '2-1' },
    { day: 1, period: 6, start: '14:50', end: '15:40', subject: '수학', grade: '2-1' },
    { day: 2, period: 1, start: '09:00', end: '09:50', subject: '수학', grade: '2-1' },
    { day: 2, period: 3, start: '11:00', end: '11:50', subject: '수학', grade: '2-3' },
    { day: 3, period: 2, start: '10:00', end: '10:50', subject: '수학', grade: '2-2' },
    { day: 3, period: 4, start: '12:00', end: '12:50', subject: '수학', grade: '2-1' },
    { day: 4, period: 2, start: '10:00', end: '10:50', subject: '수학', grade: '2-2' },
    { day: 5, period: 1, start: '09:00', end: '09:50', subject: '수학', grade: '2-3' },
    { day: 5, period: 2, start: '10:00', end: '10:50', subject: '수학', grade: '2-1' },
    { day: 5, period: 3, start: '11:00', end: '11:50', subject: '수학', grade: '2-2' },
    { day: 5, period: 4, start: '12:00', end: '12:50', subject: '수학', grade: '2-1' },
];

const FIXED_SCHEDULE = [
    { name: '교무회의',    days: [3],    time: '16:00', location: '회의실' },
    { name: '학부모 상담', days: [4],    time: '17:00', location: '상담실' },
    { name: '학년 협의회', days: [5],    time: '15:00', location: '2학년 교무실' },
    { name: '방과후 수업', days: [2, 4], time: '19:00', location: '301호' },
];

const PERIOD_TIMES = [
    { start: '09:00', end: '09:50' },
    { start: '10:00', end: '10:50' },
    { start: '11:00', end: '11:50' },
    { start: '12:00', end: '12:50' },
    { start: '13:50', end: '14:40' },
    { start: '14:50', end: '15:40' },
    { start: '15:50', end: '16:40' },
];

const PERIOD_ROW_IDX = [0, 1, 2, 3, 5, 6, 7];

const tick = () => {
    const now    = getKST();
    const dow    = now.getDay();
    const nowMin = now.getHours() * 60 + now.getMinutes();

    // 홈 카드: 실제 시간표/고정 일정 데이터로 렌더링
    renderNextClass();
    renderUpcomingSchedule();

    const tbody = document.querySelector('.timetable tbody');
    if (tbody) {
        let currentPeriodIdx = -1;
        PERIOD_TIMES.forEach((p, i) => {
            if (toMinutes(p.start) <= nowMin && nowMin < toMinutes(p.end)) currentPeriodIdx = i;
        });
        const allRows = tbody.querySelectorAll('tr');
        allRows.forEach(row => {
            row.classList.remove('current-period-row');
            row.querySelectorAll('.class-cell').forEach(c => c.classList.remove('current-class'));
        });
        if (currentPeriodIdx >= 0 && dow >= 1 && dow <= 5) {
            const rowIdx = PERIOD_ROW_IDX[currentPeriodIdx];
            if (allRows[rowIdx]) {
                allRows[rowIdx].classList.add('current-period-row');
                const cell = allRows[rowIdx].cells[dow + 1];
                if (cell) cell.querySelector('.class-cell')?.classList.add('current-class');
            }
        }
    }
};

setInterval(tick, 60000);
tick();

const ALL_NOTICES = [
    { title: '긴급 교무회의 일정 변경 안내',           date: '2026-05-26', isNew: true,  content: '5월 26일(화) 오후 교무회의 일정이 기존 15:00에서 16:00으로 변경되었습니다. 모든 교직원은 회의실 B에 참석 바랍니다.' },
    { title: '2학기 교육과정 설명회 안내',              date: '2026-05-25', isNew: true,  content: '2026학년도 2학기 교육과정 설명회가 6월 10일(수) 오후 2시, 대강당에서 개최됩니다. 담당 교과 자료를 미리 준비해 주시기 바랍니다.' },
    { title: '2026 1학기 중간고사 일정 안내',           date: '2026-05-23', isNew: false, content: '중간고사 일정: 6월 2일(월) ~ 6월 4일(수). 시험 범위 및 출제 기준은 각 교과 담당 교사를 통해 공지됩니다.' },
    { title: '학부모 상담 주간 운영 안내',               date: '2026-05-21', isNew: false, content: '학부모 상담 주간: 5월 26일(월) ~ 5월 30일(금). 담임 교사별 사전 예약제로 운영되며, 상담 신청은 학교 홈페이지를 통해 접수 가능합니다.' },
    { title: '온라인 학습 시스템 점검 예정',             date: '2026-05-19', isNew: false, content: '5월 21일(수) 오전 2시 ~ 4시 사이 EduTrack 시스템 정기 점검이 진행됩니다. 해당 시간에는 서비스 이용이 일시 중단될 수 있습니다.' },
    { title: '2026학년도 수학여행 계획 안내',            date: '2026-05-15', isNew: false, content: '2학년 수학여행 일정: 6월 18일(수) ~ 6월 20일(금), 경주 방면. 세부 일정 및 준비물 안내문은 담임 선생님을 통해 배부됩니다.' },
    { title: '학생 건강검진 실시 안내',                  date: '2026-05-12', isNew: false, content: '1학년 학생 건강검진이 5월 22일(목) 오전 9시부터 학교 내 보건실에서 실시됩니다. 검진 당일 공복 상태로 등교 바랍니다.' },
    { title: '교내 스포츠 대회 일정 공고',               date: '2026-05-08', isNew: false, content: '교내 체육대회: 5월 29일(목) 오전 9시 ~ 오후 4시, 운동장. 학급 대항 종목: 축구, 줄다리기, 계주. 각 학급 담임 선생님께서 참가 신청서를 취합해 주세요.' },
    { title: '도서관 이용 시간 변경 안내',               date: '2026-05-05', isNew: false, content: '중간고사 기간(6월 2일 ~ 4일) 동안 도서관 이용 시간이 오전 7시 30분 ~ 오후 9시로 연장 운영됩니다. 열람실 좌석 예약은 사전 신청 바랍니다.' },
    { title: '2026학년도 1학기 학급 임원 선출 결과',     date: '2026-04-30', isNew: false, content: '각 학급 임원 선출 결과가 확정되었습니다. 회장단은 5월 2일부터 공식 활동을 시작하며, 전체 임원 명단은 학교 게시판에서 확인하실 수 있습니다.' },
    { title: '교원 연수 일정 안내 (5월)',                date: '2026-04-25', isNew: false, content: '5월 교원 직무 연수가 5월 7일(수) ~ 5월 9일(금), 연수원에서 진행됩니다. 참가 대상 교원은 개별 안내문을 확인하시고 사전 신청 바랍니다.' },
    { title: '학교 홈페이지 개편 안내',                  date: '2026-04-20', isNew: false, content: '학교 공식 홈페이지가 5월 1일부로 새롭게 개편됩니다. 기존 즐겨찾기 주소는 그대로 유지되며, 일부 메뉴 구조가 변경될 예정입니다.' },
].sort((a, b) => b.date.localeCompare(a.date));

const ALL_UPDATES = [
    { title: '2학년 2반 수학 진도 업데이트',            date: '2026-05-26', isNew: true,  content: '2학년 2반 수학 수업이 3단원 "이차함수" 진도까지 완료되었습니다. 다음 수업은 4단원 "경우의 수"로 진행될 예정입니다.' },
    { title: '학생 평가 자료 업로드 완료',               date: '2026-05-25', isNew: true,  content: '5월 형성평가 결과 데이터가 EduTrack에 업로드 완료되었습니다. 각 학생별 취약 개념 분석 결과를 오답노트 탭에서 확인하실 수 있습니다.' },
    { title: '1학년 1반 영어 단어 시험 결과 등록',       date: '2026-05-23', isNew: false, content: '1학년 1반 영어 단어 테스트(5월 22일 실시) 채점 결과가 등록되었습니다. 평균 점수: 82점.' },
    { title: '3학년 수학 심화 문제지 배포 완료',         date: '2026-05-21', isNew: false, content: '3학년 대상 수학 심화 학습 문제지(수능 대비 모의 세트 2회분)가 디지털 OMR 탭에 배포 완료되었습니다.' },
    { title: '국어 문학 (3-14) 배포 완료',               date: '2026-05-20', isNew: false, content: '3학년 14반 대상 국어 문학 영역 평가지가 배포되었습니다. 제출 기한은 5월 27일입니다.' },
    { title: '2학년 과학 실험 보고서 제출 현황',         date: '2026-05-18', isNew: false, content: '2학년 과학 실험 보고서 제출률: 87%. 미제출 학생 명단이 해당 담임 선생님께 전달되었습니다.' },
    { title: '학급별 독서 기록 현황 업데이트',           date: '2026-05-16', isNew: false, content: '5월 기준 학급별 독서 기록 현황이 업데이트되었습니다. 1학년 평균 2.3권, 2학년 1.8권, 3학년 1.2권입니다.' },
    { title: '수업 종료 (2-1) 보고 완료',                date: '2026-05-15', isNew: false, content: '2학년 1반 수업 보고서가 제출 및 확인 완료되었습니다.' },
    { title: '1학기 성적 입력 마감 안내 업데이트',       date: '2026-05-13', isNew: false, content: '1학기 중간고사 성적 입력 마감: 6월 10일(수). 성적 이의신청 기간: 6월 11일 ~ 6월 13일.' },
    { title: '디지털 OMR 신규 문항 등록',                date: '2026-05-10', isNew: false, content: '2학년 대상 수학 5단원 "확률" 퀴즈 15문항이 디지털 OMR에 새롭게 등록되었습니다.' },
    { title: '3학년 모의고사 성적 분석 완료',            date: '2026-05-07', isNew: false, content: '5월 모의고사 성적 분석이 완료되었습니다. 국어 평균 64점, 수학 58점, 영어 71점. 상세 분석 리포트는 각 담당 교과 교사에게 공유되었습니다.' },
    { title: '학급 오답률 분석 리포트 생성',             date: '2026-05-04', isNew: false, content: '4월 형성평가 기준 학급별 오답률 분석 리포트가 생성되었습니다. 디지털 OMR → 학급 오답률 분석 탭에서 확인 가능합니다.' },
].sort((a, b) => b.date.localeCompare(a.date));

const renderNoticeCard = () => {
    const list = document.getElementById('noticeCardList');
    if (!list) return;
    list.innerHTML = ALL_NOTICES.slice(0, 5).map(n => `
        <div class="notice-item">
            <span class="notice-text">${n.isNew ? '<span class="badge-n">N</span> ' : ''}${n.title}</span>
            <span class="notice-date">${n.date}</span>
        </div>`).join('');
};

const renderUpdateCard = () => {
    const list = document.getElementById('updateCardList');
    if (!list) return;
    list.innerHTML = ALL_UPDATES.slice(0, 4).map(u => `
        <div class="notice-item">
            <span class="notice-text">${u.isNew ? '<span class="badge-n">N</span> ' : ''}${u.title}</span>
            <span class="notice-date">${u.date}</span>
        </div>`).join('');
};

window.openNoticeListModal = () => {
    const list = document.getElementById('noticeFullList');
    if (list) {
        list.innerHTML = ALL_NOTICES.map((n, i) => `
            <div class="notice-full-item" id="nfull-${i}" onclick="toggleNoticeItem(${i})">
                <div class="notice-full-header">
                    <span class="notice-text">${n.isNew ? `<span class="badge-n" id="nbadge-${i}">N</span> ` : ''}${n.title}</span>
                    <span class="notice-date">${n.date}</span>
                </div>
                ${n.content ? `<div class="notice-full-body" id="nbody-${i}" style="display:none;">${n.content}</div>` : ''}
            </div>`).join('');
    }
    document.getElementById('noticeListModal').style.display = 'flex';
};

window.toggleNoticeItem = (i) => {
    const body = document.getElementById('nbody-' + i);
    if (body) body.style.display = body.style.display === 'none' ? 'block' : 'none';
    if (ALL_NOTICES[i].isNew) {
        ALL_NOTICES[i].isNew = false;
        document.getElementById('nbadge-' + i)?.remove();
        renderNoticeCard();
    }
};

window.openUpdateListModal = () => {
    const list = document.getElementById('updateFullList');
    if (list) {
        list.innerHTML = ALL_UPDATES.map((u, i) => `
            <div class="notice-full-item" id="ufull-${i}" onclick="toggleUpdateItem(${i})">
                <div class="notice-full-header">
                    <span class="notice-text">${u.isNew ? `<span class="badge-n" id="ubadge-${i}">N</span> ` : ''}${u.title}</span>
                    <span class="notice-date">${u.date}</span>
                </div>
                ${u.content ? `<div class="notice-full-body" id="ubody-${i}" style="display:none;">${u.content}</div>` : ''}
            </div>`).join('');
    }
    document.getElementById('updateListModal').style.display = 'flex';
};

window.toggleUpdateItem = (i) => {
    const body = document.getElementById('ubody-' + i);
    if (body) body.style.display = body.style.display === 'none' ? 'block' : 'none';
    if (ALL_UPDATES[i].isNew) {
        ALL_UPDATES[i].isNew = false;
        document.getElementById('ubadge-' + i)?.remove();
        renderUpdateCard();
    }
};

renderNoticeCard();
renderUpdateCard();

// ═══ 일정 항목 클릭 → 상세 팝업 ═══
let currentDetailTarget = null;

const openClassCellDetail = (cell) => {
    const td = cell.closest('td');
    const tr = td?.closest('tr');
    const tbody = tr?.closest('tbody');
    if (!td || !tr || !tbody) return;

    const dayColIdx = td.cellIndex;
    const rows = Array.from(tbody.querySelectorAll('tr'));
    const rowIdx = rows.indexOf(tr);
    const periodIdx = PERIOD_ROW_IDX.indexOf(rowIdx);

    const subject = cell.childNodes[0]?.textContent?.trim() || '';
    const cls = cell.querySelector('small')?.textContent?.trim() || '';

    const DAY_NAMES = ['', '월요일', '화요일', '수요일', '목요일', '금요일'];
    const dayName = DAY_NAMES[dayColIdx - 1] || '';
    const PERIOD_NAMES = ['1교시', '2교시', '3교시', '4교시', '5교시', '6교시', '7교시'];
    const periodName = periodIdx >= 0 ? PERIOD_NAMES[periodIdx] : '알 수 없음';
    const timeRange = periodIdx >= 0 ? `${PERIOD_TIMES[periodIdx].start}–${PERIOD_TIMES[periodIdx].end}` : '';

    currentDetailTarget = { type: 'class-cell', element: cell, td, periodIdx, dayColIdx };

    document.getElementById('sdModalTitle').textContent = '수업 상세 정보';
    document.getElementById('sdModalBody').innerHTML = `
        <div class="detail-row"><span class="detail-label">교시</span><span class="detail-value">${periodName}</span></div>
        <div class="detail-row"><span class="detail-label">요일</span><span class="detail-value">${dayName}</span></div>
        <div class="detail-row"><span class="detail-label">과목</span><span class="detail-value">${subject}</span></div>
        ${cls ? `<div class="detail-row"><span class="detail-label">학급</span><span class="detail-value">${cls}</span></div>` : ''}
        <div class="detail-row"><span class="detail-label">시간</span><span class="detail-value">${timeRange}</span></div>
    `;
    document.getElementById('scheduleDetailModal').style.display = 'flex';
};

const openFixedItemDetail = (item) => {
    const titleText = item.querySelector('.fixed-title')?.textContent?.trim() || '';
    const rawDetail = item.querySelector('.fixed-detail')?.textContent?.trim() || '';
    const rawLocation = item.querySelector('.fixed-location')?.textContent?.trim() || '';

    const timeMatch = rawDetail.match(/(\d{1,2}:\d{2}(?:[-–]\d{1,2}:\d{2})?)$/);
    const timeStr = timeMatch ? timeMatch[1] : '';
    const dayStr = timeStr ? rawDetail.slice(0, rawDetail.length - timeStr.length).trim() : rawDetail;

    currentDetailTarget = { type: 'fixed-item', element: item, title: titleText, day: dayStr, time: timeStr, location: rawLocation };

    document.getElementById('sdModalTitle').textContent = '고정 일정 상세 정보';
    document.getElementById('sdModalBody').innerHTML = `
        <div class="detail-row"><span class="detail-label">일정명</span><span class="detail-value">${titleText}</span></div>
        <div class="detail-row"><span class="detail-label">반복</span><span class="detail-value">${dayStr}</span></div>
        <div class="detail-row"><span class="detail-label">시간</span><span class="detail-value">${timeStr}</span></div>
        ${rawLocation ? `<div class="detail-row"><span class="detail-label">장소</span><span class="detail-value">${rawLocation}</span></div>` : ''}
    `;
    document.getElementById('scheduleDetailModal').style.display = 'flex';
};

document.querySelector('.timetable')?.addEventListener('click', e => {
    if (window.ttDeleteMode) return;
    const cell = e.target.closest('.class-cell');
    if (cell) openClassCellDetail(cell);
});

document.querySelector('.fixed-schedule-card')?.addEventListener('click', e => {
    if (window.fsDeleteMode) return;
    const item = e.target.closest('.fixed-item');
    if (item) openFixedItemDetail(item);
});

window.openEditFromDetail = () => {
    if (!currentDetailTarget) return;
    window.closeModal('scheduleDetailModal');

    if (currentDetailTarget.type === 'class-cell') {
        const { periodIdx, dayColIdx, element } = currentDetailTarget;
        const subject = element.childNodes[0]?.textContent?.trim() || '';
        const cls = element.querySelector('small')?.textContent?.trim() || '';

        document.getElementById('editTimePeriod').value = periodIdx >= 0 ? String(periodIdx) : '0';
        document.getElementById('editTimeDay').value = String(dayColIdx);
        document.getElementById('editTimeSubject').value = subject;
        document.getElementById('editTimeClass').value = cls;
        document.getElementById('editTimeModal').style.display = 'flex';
    } else {
        const { title, day, time, location } = currentDetailTarget;
        document.getElementById('editFixedTitle').value    = title;
        document.getElementById('editFixedDay').value      = day;
        document.getElementById('editFixedLocation').value = location;
        // 기존 시간 파싱 후 UI에 반영
        const tp = parseFixedTimeStr(time);
        document.getElementById('editFixedAllDay').checked  = tp.allDay;
        document.getElementById('editFixedTimeArea').style.display = tp.allDay ? 'none' : 'block';
        buildTimeSelectOptions('editFixedStartH', 'editFixedStartM', tp.startH ?? 9, tp.startM ?? 0);
        buildTimeSelectOptions('editFixedEndH',   'editFixedEndM',   tp.endH   ?? 10, tp.endM  ?? 0);
        const endCheck = document.getElementById('editFixedEndCheck');
        const endArea  = document.getElementById('editFixedEndArea');
        endCheck.checked          = !!tp.hasEnd;
        endArea.style.display     = tp.hasEnd ? 'flex' : 'none';
        document.getElementById('editFixedModal').style.display = 'flex';
    }
};

window.deleteFromDetail = () => {
    if (!currentDetailTarget) return;
    const { type, element } = currentDetailTarget;
    window.closeModal('scheduleDetailModal');

    document.getElementById('deleteConfirmText').textContent =
        type === 'class-cell' ? '이 수업을 시간표에서 삭제하시겠습니까?' : '이 고정 일정을 삭제하시겠습니까?';
    document.getElementById('deleteConfirmYes').onclick = () => {
        if (type === 'class-cell') {
            const td = element.closest('td');
            if (td) td.innerHTML = '';
        } else {
            element.remove();
        }
        currentDetailTarget = null;
        window.closeModal('deleteConfirmModal');
        window.saveScheduleToFirebase?.();
    };
    document.getElementById('deleteConfirmModal').style.display = 'flex';
};

window.submitEditTime = () => {
    const newPeriodIdx = parseInt(document.getElementById('editTimePeriod').value);
    const newDayColIdx = parseInt(document.getElementById('editTimeDay').value);
    const subject = document.getElementById('editTimeSubject').value.trim();
    const cls = document.getElementById('editTimeClass').value.trim();

    if (!subject) { alert('과목명을 입력해주세요.'); return; }
    if (!currentDetailTarget) return;

    const { td: origTd, periodIdx: origPeriodIdx, dayColIdx: origDayColIdx } = currentDetailTarget;
    const positionChanged = newPeriodIdx !== origPeriodIdx || newDayColIdx !== origDayColIdx;

    if (positionChanged) {
        const tbody = document.querySelector('.timetable tbody');
        const rows = tbody.querySelectorAll('tr');
        const targetRow = rows[PERIOD_ROW_IDX[newPeriodIdx]];
        if (targetRow?.cells[newDayColIdx]?.querySelector('.class-cell')) {
            alert('해당 교시/요일에 이미 수업이 등록되어 있습니다.');
            return;
        }
    }

    if (origTd) origTd.innerHTML = '';

    const tbody = document.querySelector('.timetable tbody');
    const rows = tbody.querySelectorAll('tr');
    const newRow = rows[PERIOD_ROW_IDX[newPeriodIdx]];
    if (newRow) {
        const newCell = newRow.cells[newDayColIdx];
        if (newCell) {
            const label = cls ? `${subject}<br><small>${cls}</small>` : subject;
            newCell.innerHTML = `<div class="class-cell">${label}</div>`;
        }
    }

    currentDetailTarget = null;
    window.closeModal('editTimeModal');
    window.saveScheduleToFirebase?.();
};

window.submitEditFixed = () => {
    const title    = document.getElementById('editFixedTitle').value.trim();
    const day      = document.getElementById('editFixedDay').value;
    const time     = readFixedTimeFromUI('editFixed');
    const location = document.getElementById('editFixedLocation').value.trim();

    if (!title) { alert('일정명을 입력해주세요.'); return; }
    if (!currentDetailTarget) return;

    const origElement = currentDetailTarget.element;
    const startTime = time.split(/[-–]/)[0].trim();

    const conflict = Array.from(document.querySelectorAll('.fixed-item')).some(item => {
        if (item === origElement) return false;
        const raw = item.querySelector('.fixed-detail')?.textContent?.trim() || '';
        const tMatch = raw.match(/(\d{1,2}:\d{2}(?:[-–]\d{1,2}:\d{2})?)$/);
        if (!tMatch) return false;
        const itemStart = tMatch[1].split(/[-–]/)[0].trim();
        const itemDay = raw.slice(0, raw.length - tMatch[0].length).trim();
        return itemDay === day && itemStart === startTime;
    });

    if (conflict) {
        alert('같은 요일/시간에 이미 등록된 일정이 있습니다.');
        return;
    }

    const titleEl = origElement.querySelector('.fixed-title');
    if (titleEl) titleEl.textContent = title;

    const detailEl = origElement.querySelector('.fixed-detail');
    if (detailEl) detailEl.textContent = `${day} ${time}`;

    let locationEl = origElement.querySelector('.fixed-location');
    if (location) {
        if (!locationEl) {
            locationEl = document.createElement('div');
            locationEl.className = 'fixed-location';
            origElement.appendChild(locationEl);
        }
        locationEl.textContent = location;
    } else if (locationEl) {
        locationEl.remove();
    }

    currentDetailTarget = null;
    window.closeModal('editFixedModal');
    window.saveScheduleToFirebase?.();
};
