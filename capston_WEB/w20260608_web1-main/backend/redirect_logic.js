import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-app.js";
import { getFirestore, doc, getDoc } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-firestore.js";

const firebaseConfig = {
    apiKey: "AIzaSyDzi7vvF4A5E_Mg9SMJrZZ8Z7RMCxgLcc4",
    authDomain: "edutrack-bfd57.firebaseapp.com",
    projectId: "edutrack-bfd57",
    storageBucket: "edutrack-bfd57.firebasestorage.app"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

const setMsg = (msg, sub = '') => {
    document.getElementById('spinner').style.display = 'none';
    document.getElementById('msg').innerText = msg;
    document.getElementById('sub').innerText = sub;
};

(async () => {
    const parts = window.location.pathname.split('/');
    const code = parts[parts.length - 1].toUpperCase();

    if (!code || code === 'E') {
        setMsg('잘못된 링크입니다.', '링크를 다시 확인해주세요.');
        return;
    }

    try {
        const snap = await getDoc(doc(db, "short_links", code));
        if (!snap.exists()) {
            setMsg('존재하지 않는 링크입니다.', '링크가 만료되었거나 잘못되었습니다.');
            return;
        }
        const { grade, class: cls, name, number, testId } = snap.data();
        const params = new URLSearchParams({ grade, class: cls });
        if (name) params.set('name', name);
        if (number) params.set('number', String(number));
        // testId를 반드시 전달해야 student_test.html이 "특정 시험 지정 모드"로 동작한다.
        // 누락 시 "자동 선택 모드"가 되어, 이탈로 차단된 시험을 건너뛰고 다른 배포 시험으로 입장하는 버그 발생.
        if (testId) params.set('testId', testId);
        window.location.replace(`/frontend/student_test.html?${params.toString()}`);
    } catch (e) {
        setMsg('오류가 발생했습니다.', '잠시 후 다시 시도해주세요.');
    }
})();
