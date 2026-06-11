import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-app.js";
import { getFirestore, collection, addDoc, getDocs, onSnapshot, query, where, doc, getDoc, setDoc } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-firestore.js";

// 파이어베이스 프로젝트 연동
const firebaseConfig = { 
    apiKey: "AIzaSyDzi7vvF4A5E_Mg9SMJrZZ8Z7RMCxgLcc4", 
    authDomain: "edutrack-bfd57.firebaseapp.com", 
    projectId: "edutrack-bfd57",
    storageBucket: "edutrack-bfd57.firebasestorage.app" 
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

let currentSubject = "";
let currentTestId = "";
let currentTeacherId = "";
let teacherAnswers = {};
let currentRadarKeywords = null;
let loggedInStudentName = "";
let loggedInStudentGrade = "";
let loggedInStudentClass = "";
let loggedInStudentNumber = 0;

// 🚨 부정행위 통제용 변수
let cheatCount = 0;
let isProcessingCheating = false;
let cheatCooldown = false; 

// 🌟 스텝형 팝업 OMR 제어 변수
let currentQIndex = 1;
let totalQCount = 5;
let studentAnswers = {};

// 1. 학생 로그인 및 입장
window.enterStudentRoom = async () => {
    const grade = document.getElementById('stdGrade').value;
    const cls = document.getElementById('stdClass').value;
    const number = parseInt(document.getElementById('stdNumber').value);
    const name = document.getElementById('stdName').value;

    if(!grade || !cls || !name) return alert("학년, 반, 이름을 모두 입력해주세요!");
    if(!number || number < 1 || number > 40) return alert("번호를 정확히 입력해주세요 (1~40).");

    loggedInStudentGrade = grade;
    loggedInStudentClass = cls;
    loggedInStudentNumber = number;
    loggedInStudentName = name;

    document.getElementById('displayStudentInfo').innerText = `👤 ${grade}학년 ${cls}반 ${number}번 ${name}`;
    document.getElementById('studentLoginOverlay').style.display = 'none';

    // 변수 초기화
    cheatCount = 0;
    isProcessingCheating = false;
    cheatCooldown = false;
    currentQIndex = 1;
    studentAnswers = {};

    // 대시보드 학생 목록에 즉시 등록
    try {
        await setDoc(doc(db, 'students', `${grade}_${cls}_${number}_${name}`), {
            name, grade, class: cls, number, status: '응시 대기', enteredAt: Date.now()
        }, { merge: true });
    } catch(e) { console.error('학생 등록 실패:', e); }

    listenLatestTest();
};

// 2. 실시간 시험지 수신 — deployments 컬렉션에서 이 학생의 반에 활성화된 시험을 찾음
function listenLatestTest() {
    const gradeClass = `${loggedInStudentGrade}_${loggedInStudentClass}`;
    console.log("[listenLatestTest] 쿼리 gradeClass:", gradeClass);
    const q = query(collection(db, "deployments"), where("gradeClass", "==", gradeClass));

    onSnapshot(q, async (snapshot) => {
        try {
            const active = snapshot.docs.find(d => d.data().status === "active");

            if (!active) {
                document.getElementById('subject-title').innerText = "현재 이 반에 배포된 시험이 없습니다.";
                document.getElementById('testImage').style.display = 'none';
                teacherAnswers = {};
                currentSubject = "";
                currentTestId = "";
                totalQCount = 5;
                return;
            }

            const dep = active.data();
            currentTestId = dep.testId;
            currentTeacherId = dep.teacherId || "";

            const testDocSnap = await getDoc(doc(db, "tests", dep.testId));
            if (!testDocSnap.exists()) {
                document.getElementById('subject-title').innerText = "시험 데이터를 찾을 수 없습니다.";
                return;
            }

            const data = testDocSnap.data();
            currentSubject = data.subject;
            teacherAnswers = data.answers || {};
            currentRadarKeywords = data.radarKeywords || null;

            document.getElementById('subject-title').innerText = currentSubject;

            if (data.imageUrl) {
                document.getElementById('testImage').src = data.imageUrl;
                document.getElementById('testImage').style.display = 'block';
            }

            totalQCount = Object.keys(teacherAnswers).length || 5;
            initExamUI(totalQCount);

            // 이미 제출한 시험인지 Firestore에서 확인 (재제출 방지)
            const prevSnap = await getDocs(query(
                collection(db, 'student_scores'),
                where('testId', '==', dep.testId),
                where('name', '==', loggedInStudentName),
                where('grade', '==', loggedInStudentGrade),
                where('class', '==', loggedInStudentClass)
            ));
            if (!prevSnap.empty) {
                const submitBtn = document.querySelector('.submit-btn');
                if (submitBtn) {
                    submitBtn.innerText = '이미 제출 완료된 시험입니다';
                    submitBtn.disabled = true;
                    submitBtn.style.backgroundColor = '#8B95A1';
                    submitBtn.onclick = null;
                }
            }
        } catch (err) {
            console.error("[listenLatestTest] 오류:", err);
            document.getElementById('subject-title').innerText = `오류: ${err.message}`;
        }
    }, (err) => {
        // onSnapshot 자체 에러 (보안 규칙 거부 등)
        console.error("[listenLatestTest] 쿼리 오류:", err);
        document.getElementById('subject-title').innerText = `연결 오류: ${err.message}`;
    });
}

// 3. 우측 요약 패널 초기화 및 UI 세팅
function initExamUI(total) {
    document.getElementById('controlBar').style.display = 'flex';
    const summaryContainer = document.getElementById('summary-container');
    summaryContainer.innerHTML = '';
    
    for (let i = 1; i <= total; i++) {
        let item = document.createElement('div');
        item.className = 'summary-item';
        item.innerHTML = `<span>${i}번 문항</span> <div class="summary-ans-badge" id="summary-badge-${i}">-</div>`;
        summaryContainer.appendChild(item);
    }
    updateControlBar();
}

// 4. 단일 문항 조작 바 (다음 문제 로직)
function updateControlBar() {
    document.getElementById('current-q-label').innerText = `${currentQIndex}번 문항`;
    
    if (currentQIndex >= totalQCount) {
        document.getElementById('btn-next-q').style.display = 'none'; // 마지막 문제면 숨김
    } else {
        document.getElementById('btn-next-q').style.display = 'inline-block';
    }
}

window.nextQuestion = () => {
    if(currentQIndex < totalQCount) {
        currentQIndex++;
        updateControlBar();
    }
};

// 5. 정답 입력 팝업 로직
window.openAnswerPopup = () => {
    document.getElementById('popup-q-label').innerText = `${currentQIndex}번 문항 정답 선택`;
    
    document.querySelectorAll('input[name="popup-radio"]').forEach(r => r.checked = false);
    if(studentAnswers[currentQIndex]) {
        const existingRadio = document.getElementById(`pr_${studentAnswers[currentQIndex]}`);
        if(existingRadio) existingRadio.checked = true;
    }
    
    document.getElementById('answerPopup').style.display = 'flex';
};

window.closeAnswerPopup = () => {
    document.getElementById('answerPopup').style.display = 'none';
};

window.saveAnswerPopup = () => {
    const selected = document.querySelector('input[name="popup-radio"]:checked');
    if(!selected) return alert("정답 번호를 클릭하여 선택해주세요!");

    // 답안 메모리에 저장
    studentAnswers[currentQIndex] = selected.value;

    // 우측 패널 뱃지 업데이트
    const badge = document.getElementById(`summary-badge-${currentQIndex}`);
    badge.innerText = selected.value;
    badge.style.background = '#3182F6';
    badge.style.color = '#fff';

    closeAnswerPopup();
};

// 6. 부정행위 감지 센서 및 강제 퇴장 로직
async function triggerCheatingSystem(reason) {
    if (isProcessingCheating || !loggedInStudentName || cheatCooldown) return;
    
    cheatCooldown = true; 
    cheatCount++;

    if (cheatCount === 1) {
        setTimeout(() => {
            alert(`⚠️ [부정행위 경고 1회]\n\n허가되지 않은 행동(${reason})이 감지되었습니다.\n한 번만 더 이탈하거나 단축키(Alt, Ctrl)를 누르면 시험방에서 강제 퇴장당합니다.`);
            window.focus(); 
            setTimeout(() => { cheatCooldown = false; }, 1000);
        }, 100);
    } 
    else if (cheatCount >= 2) {
        isProcessingCheating = true;
        setTimeout(async () => {
            alert(`🚨 [시험 강제 퇴장]\n\n부정행위 2회 누적으로 인해 즉시 강제 퇴장 처리됩니다.\n이 기록은 교사용 대시보드에 실시간 보고됩니다.`);
            try {
                await addDoc(collection(db, "student_scores"), {
                    name: loggedInStudentName,
                    grade: loggedInStudentGrade,
                    class: loggedInStudentClass,
                    number: loggedInStudentNumber,
                    score: "부정행위",
                    subject: currentSubject || "단원평가",
                    testId: currentTestId,
                    teacherId: currentTeacherId,
                    timestamp: Date.now(),
                    keywords: ["#부정행위감지", "#강제퇴장조치"],
                    seteuk: "시험 응시 도중 허가되지 않은 특수키(Alt/Ctrl) 사용 또는 브라우저 탭 전환이 2회 적발되어 시스템에 의해 강제 퇴장 및 실시간 부정행위 등록 처리됨.",
                    answers: {}
                });
                await setDoc(doc(db, 'students', `${loggedInStudentGrade}_${loggedInStudentClass}_${loggedInStudentName}`), {
                    status: '부정행위'
                }, { merge: true });
            } catch(e) {
                console.error("부정행위 전송 실패:", e);
            } finally {
                loggedInStudentName = "";
                document.getElementById('stdGrade').value = "";
                document.getElementById('stdClass').value = "";
                document.getElementById('stdName').value = "";
                document.getElementById('displayStudentInfo').innerText = "👤 정보 입력 대기중";
                document.getElementById('studentLoginOverlay').style.display = 'flex';
                
                cheatCount = 0;
                isProcessingCheating = false;
                cheatCooldown = false;
            }
        }, 100);
    }
}

window.addEventListener('keydown', (e) => {
    if (e.key === 'Alt' || e.key === 'Control' || e.altKey || e.ctrlKey) {
        e.preventDefault(); 
        triggerCheatingSystem("특수 단축키(Alt / Ctrl) 입력");
    }
});

document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
        triggerCheatingSystem("시험 화면 이탈/탭 전환");
    }
});

// 7. 최종 답안 제출 및 실시간 채점 로직
window.submitTest = async () => {
    if (Object.keys(studentAnswers).length < totalQCount) {
        return alert("⚠️ 아직 답안을 입력하지 않은 문항이 있습니다!\n우측 요약 패널에서 '-'로 표시된 문항을 확인하세요.");
    }

    const btn = document.querySelector('.submit-btn');
    btn.innerText = "채점 및 전송 중..."; btn.style.backgroundColor = "#8B95A1";

    let correctCount = 0;
    for (let i = 1; i <= totalQCount; i++) {
        if (String(studentAnswers[i]) === String(teacherAnswers[i])) {
            correctCount++;
        }
    }
    const calculatedScore = Math.round((correctCount / totalQCount) * 100);

    try {
        const scoreDoc = {
            name: loggedInStudentName,
            grade: loggedInStudentGrade,
            class: loggedInStudentClass,
            number: loggedInStudentNumber,
            score: calculatedScore,
            subject: currentSubject,
            testId: currentTestId,
            teacherId: currentTeacherId,
            timestamp: Date.now(),
            keywords: ["#디지털OMR", calculatedScore >= 80 ? "#상위권성취" : "#집중개선요망"],
            seteuk: `${currentSubject} 평가에서 전체 ${totalQCount}문항 중 ${correctCount}문항을 맞춰 성취도 ${calculatedScore}점을 획득함.`,
            answers: studentAnswers
        };
        if (currentRadarKeywords) scoreDoc.radarKeywords = currentRadarKeywords;
        await addDoc(collection(db, "student_scores"), scoreDoc);

        // 학생 목록 상태 업데이트
        await setDoc(doc(db, 'students', `${loggedInStudentGrade}_${loggedInStudentClass}_${loggedInStudentNumber}_${loggedInStudentName}`), {
            status: '제출 완료', score: calculatedScore, submittedAt: Date.now()
        }, { merge: true });

        alert(`🎉 [${currentSubject}] 제출 완료!\n\n내 점수: ${calculatedScore}점\n\n선생님 대시보드에 즉시 업데이트되었습니다.`);
        
        btn.innerText = "제출 완료됨";
        btn.onclick = null;
    } catch(e) {
        alert(`전송 오류: ${e.message}`);
    } finally {
        if(btn.innerText !== "제출 완료됨") btn.innerText = "🚀 최종 답안 제출하기";
    }
};