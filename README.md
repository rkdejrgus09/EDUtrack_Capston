[README_EduTrack.md](https://github.com/user-attachments/files/28833744/README_EduTrack.md)
# 🎓 에듀트랙 (EduTrack)
> **Vision AI 기반 지능형 데이터 파이프라인 및 디지털 OMR 평가 플랫폼**
>
> 기존 교육 현장의 아날로그 평가 데이터 휘발 문제를 해결하고, 교사의 행정 업무(채점, 전산 입력, 생활기록부 작성)를 전면 자동화하기 위한 지능형 에듀테크 솔루션입니다.

<br>

## 📌 프로젝트 개요 (Project Overview)
* **개발 기간**: 2026.03 ~ 2026.06 (4개월)
* **개발 인원**: 3명 (컴퓨터공학과 캡스톤 디자인 프로젝트)
* **핵심 목표**: 
  1. 교사의 시험 채점 및 전산 입력 시간 단축 (행정 부담 30% 경감)
  2. 휘발되는 수기 평가 데이터의 클라우드 디지털 자산화
  3. 누적 데이터를 기반으로 한 학생별 맞춤형 취약점 분석 및 AI 세특 초안 생성

<br>

## 🛠 기술 스택 및 아키텍처 (Tech Stack & Architecture)

### 시스템 구성도 (System Topology)
```
[교사 웹 대시보드] (HTML/CSS/JS, Chart.js) 🌐
       │
       ▼ (REST API / 실시간 동기화)
[Google Cloud Platform] ☁️ ─── [Firebase Cloud Firestore] (NoSQL DB) 🗄️
       ▲ (데이터 파싱 및 채점 요청)
       │
[Python AI 백엔드 서버] (Flask) 🐍 ─── [Vision AI / LLM 알고리즘] 🤖
       ▲
       │ (디지털 OMR 답안 제출 / 실시간 트래킹)
[학생용 모바일 앱] (Android Studio, Kotlin) 📱
```

### 상세 기술 스택
* **Cloud & Infrastructure:** Google Cloud Platform (GCP)
* **Database:** Firebase Cloud Firestore (NoSQL 실시간 동기화 데이터베이스)
* **Backend:** Python (Flask)
* **Frontend (Web):** HTML5, CSS3, JavaScript (ES6+), Chart.js
* **Frontend (App):** Android Studio, Kotlin
* **Version Control:** Git, GitHub

<br>

## ✨ 핵심 구현 기능 (Key Features)

### 1. 교사용 웹 관제 대시보드 (`Frontend-Web`)
* **Zero-Typing 출제 시스템:** 기존 종이 시험지(PDF/이미지)를 타이핑 과정 없이 통째로 업로드하여 디지털 평가방을 생성하는 가속화 프로세스 구현.
* **실시간 모니터링 및 시각화:** `Chart.js`를 활용하여 학급별 문항 오답률 추이 및 학생별 다차원 성취도 지표(레이더 차트)를 대시보드 상에 실시간 시각화.
* **AI 생활기록부 비서:** 누적된 정/오답 객체 데이터를 파싱하여 주관적 서술을 배제한 'AI 세특(세부능력 및 특기사항) 초안 및 성취 키워드 자동 산출' 기능 구현.

### 2. 학생용 디지털 OMR 모바일 애플리케이션 (`Frontend-App`)
* **다이렉트 OMR 평가방 입장:** QR 코드 스캔 및 고유 링크 연동을 통해 별도의 복잡한 절차 없이 가상 OMR 카드 마킹 화면으로 즉시 진입.
* **부정행위 실시간 감지 알고리즘:** 응시 중 발생하는 화면 이탈, 앱 백그라운드 전환, 허가되지 않은 단축키(`Alt`, `Ctrl`) 입력을 실시간 트래킹하여 자동 경고 및 2회 적발 시 강제 퇴장(0점) 처리 로직 구현.

### 3. AI 실시간 채점 백엔드 파이프라인 (`Backend`)
* **0.1초 즉시 판독 엔진:** 학생이 OMR 답안을 제출하는 즉시 Python 백엔드 채점 알고리즘과 교차 검증하여 지연 없이 자동 채점 완료.
* **개념 태그 자동 매핑:** 단순 총점 합산을 넘어, 문항마다 사전에 연동된 단원별 '개념 태그(예: 데이터베이스 정규화, 네트워크 계층)'를 분석 데이터와 매핑하는 로직 설계.

<br>

## 🗄️ 데이터베이스 설계 (Database Architecture)
Firebase Cloud Firestore(NoSQL)의 특성을 살려 트래픽 병목을 최소화하고 데이터 유실을 방지할 수 있도록 **Collection-Document 계층형 아키텍처**로 설계했습니다.

```
📁 Users (사용자 컬렉션: 권한 관리)
 └─ 📄 {uid} (문서 ID)
     ├─ 🔑 role : "teacher" | "student" | "admin" (RBAC 권한 구조)
     ├─ 📧 email : 계정 이메일
     └─ ✅ status : "승인대기" | "활성" (교사 가입 관리 프로세스)

📁 Exams (평가/시험 컬렉션: 출제 데이터)
 └─ 📄 {exam_id} (문서 ID)
     ├─ 👤 teacher_id : 출제자 UID 참조
     ├─ 📄 title : 시험지 제목
     └─ 📋 questions : [ { "num": 1, "answer": 3, "tag": "방정식" }, ... ] (배열 객체)

📁 Submissions (OMR 제출 컬렉션: 응시 결과)
 └─ 📄 {submission_id} (문서 ID)
     ├─ 📄 exam_id : 응시한 시험 ID 참조
     ├─ 👤 student_id : 응시자 UID 참조
     ├─ 🖊️ answers : [1, 3, 2, 4, 2] (학생 마킹 답안 배열)
     ├─ 💯 score : 85 (자동 채점 총점)
     └─ ⚠️ is_cheated : false (부정행위 감지 여부 Boolean)

📁 Analytics (학습 분석 컬렉션: 세특 및 취약점 데이터)
 └─ 📄 {student_id} (문서 ID: UID와 동일)
     ├─ 🏷️ weak_concepts : ["함수", "확률"] (취약 개념 태그 리스트 배열)
     └─ 💡 ai_keywords : ["논리력 우수", "계산 실수"] (생기부용 성취 키워드 배열)
```

<br>

## 🧑‍💻 담당 역할 및 기여도 (My Role & Contribution)
* **담당 역할:** 프로젝트 매니저(PM) 및 시스템 아키텍처/DB 설계 (**기여도 33%**)
* **핵심 성과:**
  * 공식 팀장 직책은 아니었으나, 아이디어 빌딩부터 유저 시나리오 구상 및 기능 요구사항 명세서(PRD) 작성을 주도하며 실질적인 PM 역할 수행.
  * 복잡한 웹-앱-AI 서버 간의 데이터 트래픽 파이프라인을 기획하고, 분산 환경에서 세션 유지 및 데이터 무결성이 보장되도록 전체 시스템 구조(Architecture Topology) 기획.
  * 개발 착수 전 NoSQL 스키마 정규화 및 아키텍처 명세서를 선제적으로 구축하여 팀 내 소통 비용과 개발 혼선을 획기적으로 방지.

<br>

## 📈 프로젝트 성과 및 회고 (Project Conclusion)
* **성과**: 복잡한 비즈니스 요구사항(교사 행정 업무 간소화)을 데이터 모델과 소프트웨어 아키텍처로 명확히 번역해내는 설계 역량을 증명했습니다.
* **회고**: 다수의 클라이언트 환경에서 실시간으로 대량의 OMR 데이터가 인입될 때 발생할 수 있는 데이터 정합성 문제를 해결하기 위해 NoSQL의 데이터 구조를 끊임없이 고도화했습니다. 이 경험을 통해 기술의 개별 기능 구현보다 **'전체 시스템의 유기적인 연결 구조와 인프라의 안정성'**이 중요함을 깨달았으며, 기술과 서비스 운영을 균형 있게 조율하는 실무형 PM으로 성장하는 발판이 되었습니다.
