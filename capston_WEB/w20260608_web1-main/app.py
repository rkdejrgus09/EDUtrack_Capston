from flask import Flask, jsonify, request
from flask_cors import CORS
import firebase_admin
from firebase_admin import credentials, firestore
import os

app = Flask(__name__)
CORS(app)

# 🌟 파이어베이스 연동 (기존과 동일, 유지)
try:
    cred = credentials.Certificate("firebase_key.json")
    if not firebase_admin._apps:
        firebase_admin.initialize_app(cred)
    db = firestore.client()
    print("✅ 파이어베이스 연동 완료! (초고속 스마트 OMR 모드)")
except Exception as e:
    print(f"❌ 파이어베이스 연동 오류: {e}")

# 🌟 세특 자동 생성용 딕셔너리 (기존과 동일, 유지)
SETEUK_DICTIONARY = {
    "excellent": "해당 단원의 핵심 개념을 완벽히 이해하고 있으며, 복잡한 문제에서도 논리적 추론을 통해 정답을 도출하는 능력이 매우 우수함.",
    "good": "기본적인 문제 해결 능력을 갖추고 있으나, 특정 유형의 응용 문제에서 사소한 실수가 발견되어 문제 풀이 시 꼼꼼한 검토가 요구됨.",
    "needs_work": "개념 이해가 다소 부족하여 반복적인 오답이 발생하므로, 기초 개념 학습 및 오답 노트 작성을 통한 보완이 강력히 권장됨."
}

@app.route('/')
def home(): 
    return "🚀 에듀트랙 스마트 OMR 채점 서버 작동 중!"

# =================================================================
# 🌟 [신규] 학생의 디지털 OMR 제출을 받아 즉시 채점하는 API
# =================================================================
@app.route('/api/submit_test', methods=['POST'])
def submit_test():
    data = request.get_json()
    subject = data.get('subject')        # 예: "2026 수학 1단원 형성평가"
    student_name = data.get('student_name') # 예: "홍길동"
    student_answers = data.get('answers')   # 예: {"1": "3", "2": "1", "3": "4", "4": "5", "5": "2"}

    if not subject or not student_name or not student_answers:
        return jsonify({"status": "error", "message": "데이터가 누락되었습니다."}), 400

    try:
        # 1. DB에서 교사가 올린 정답지(Answer Key) 가져오기
        tests_ref = db.collection('tests').where('subject', '==', subject).limit(1).stream()
        test_doc = next(tests_ref, None)

        if not test_doc:
            return jsonify({"status": "error", "message": "해당 시험의 정답 데이터를 찾을 수 없습니다."}), 404

        test_data = test_doc.to_dict()
        answer_key = test_data.get('answers', {}) # 교사가 세팅한 정답

        # 2. 0.1초 초고속 자동 채점 로직
        score = 0
        total_questions = len(answer_key)
        points_per_q = 100 / total_questions if total_questions > 0 else 20 # 5문제면 문제당 20점

        for q_num, correct_ans in answer_key.items():
            # 학생 제출 답안과 교사 정답을 문자열로 통일하여 비교
            if str(student_answers.get(str(q_num))) == str(correct_ans):
                score += points_per_q

        score = int(score) # 깔끔하게 정수로 변환

        # 3. 점수에 따른 세특/키워드 로컬 매핑
        if score >= 90:
            keywords, seteuk = ["#개념완벽이해", "#논리적추론우수"], SETEUK_DICTIONARY["excellent"]
        elif score >= 70:
            keywords, seteuk = ["#개념이해양호", "#응용력보완필요"], SETEUK_DICTIONARY["good"]
        else:
            keywords, seteuk = ["#기초학습필요", "#오답노트필수"], SETEUK_DICTIONARY["needs_work"]

        # 4. 채점 결과를 DB(교사 대시보드)로 쏘기
        db.collection('student_scores').document(student_name).set({
            'name': student_name, 
            'subject': subject, 
            'score': score,
            'student_answers': student_answers, # 학생이 누른 오답 데이터도 저장 (오답노트용)
            'keywords': keywords, 
            'seteuk': seteuk, 
            'status': '스마트 OMR 채점 완료',
            'updatedAt': firestore.SERVER_TIMESTAMP
        })

        print(f"🎯 [{student_name}] 채점 완료: {score}점")

        return jsonify({
            "status": "success", 
            "message": f"{student_name} 학생 채점 완료", 
            "score": score
        })

    except Exception as e:
        print(f"❌ 채점 중 오류 발생: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500


if __name__ == '__main__':
    app.run(debug=os.environ.get('FLASK_DEBUG', 'False') == 'True', host='0.0.0.0', port=5000)