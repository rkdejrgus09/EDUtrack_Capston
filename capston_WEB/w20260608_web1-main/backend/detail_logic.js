import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-app.js";
import { getFirestore, collection, getDocs, query, orderBy, limit } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-firestore.js";

const firebaseConfig = { apiKey: "AIzaSyARu5lhGNWTF20f42YLquQQZeRM9r3H0Cw", projectId: "edutrack-c5053" };
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

async function loadData() {
    try {
        const q = query(collection(db, "evaluations"), orderBy("date", "desc"), limit(1));
        const snap = await getDocs(q);
        if (!snap.empty) {
            const data = snap.docs[0].data();
            document.getElementById('keywordContainer').innerHTML = data.ai_keywords.map(kw => `<span class="badge">${kw}</span>`).join('');
            document.getElementById('weaknessContainer').innerText = data.ai_weakness;
        }
    } catch(e) { console.error(e); }
}
loadData();

document.getElementById("pdfBtn").addEventListener("click", () => {
    const element = document.getElementById("reportArea");
    html2pdf().from(element).save('에듀트랙_리포트.pdf');
});