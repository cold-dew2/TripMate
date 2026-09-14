🗺️ TripMate

여행지 탐색부터 여행을 함께할 소모임까지 한 곳에서 찾을 수 있는 여행 커뮤니티 서비스

TripMate는 여행지를 탐색하고, 관심 있는 여행지를 기반으로 여행 소모임을 찾아 다른 사용자와 함께 여행을 계획할 수 있도록 만든 여행 정보 및 소모임 서비스입니다.

한국관광공사 관광 데이터를 기반으로 여행지 정보를 제공하고, 여행지 상세 정보와 리뷰를 확인할 수 있으며, 지역 및 조건에 따라 소모임을 탐색하고 참여할 수 있도록 구성했습니다.

⸻

✨ 주요 기능

🏠 홈

* 인기 여행지 조회
* 인기 소모임 조회
* 여행 테마별 카테고리 제공
* 여행지 및 소모임 상세 페이지 이동

📍 여행지

* 여행지 목록 조회
* 지역별 필터링
* 여행지 상세 정보 조회
* 여행지 이용 정보 제공
* 여행지 리뷰 조회
* 여행지 평점 제공
* 무한 스크롤을 통한 목록 탐색

👥 소모임

* 소모임 목록 조회
* 지역 및 조건별 필터링
* 소모임 검색
* 무한 스크롤을 통한 목록 탐색
* 소모임 상세 정보 조회
* 소모임 생성 기능
* 로그인 사용자의 내 소모임 조회

👤 사용자

* 회원가입
* 아이디 중복 확인
* 로그인
* 사용자 정보 조회
* 마이페이지
* 작성/참여 리뷰 조회

🤖 AI 활용

* AI 기반 여행지 검색/추천 API
* AI 기반 여행지 상세 정보
* AI 기반 소모임 검색/추천 API

🌐 다국어

* 한국어 / 영어 지원
* i18next 기반 다국어 처리

⸻

🛠️ 기술 스택

Frontend

기술	사용 목적
React	UI 개발
TypeScript	정적 타입 관리
Vite	개발 서버 및 빌드
React Router	페이지 라우팅
TanStack Query	서버 상태 및 비동기 데이터 관리
React Hook Form	폼 상태 관리
i18next	다국어 처리
CSS	UI 스타일링

Backend

기술	사용 목적
Java	백엔드 개발
Spring Boot	REST API 서버
Spring Security	인증 및 보안
JWT	사용자 인증
MyBatis	데이터베이스 접근
MySQL	데이터베이스
Swagger / OpenAPI	API 문서화
Gemini API	AI 기능
한국관광공사 API	관광 데이터 제공

⸻

🏗️ 시스템 구조

┌─────────────────────────────────────────────┐
│                  TripMate                   │
└─────────────────────────────────────────────┘
                      │
          ┌───────────┴───────────┐
          │                       │
      Frontend                Backend
       React                Spring Boot
       TypeScript              Java
       Vite                  MyBatis
          │                       │
          │ HTTP / REST API       │
          └───────────┬───────────┘
                      │
               ┌──────┴──────┐
               │             │
             MySQL       External API
                           │
                 ┌─────────┴─────────┐
                 │                   │
             관광공사 API          Gemini API

⸻

📁 프로젝트 구조

TripMate-main/
│
├── frontend/
│   ├── src/
│   │   ├── features/
│   │   │   ├── auth/
│   │   │   ├── guide/
│   │   │   ├── home/
│   │   │   ├── moim/
│   │   │   ├── my/
│   │   │   └── place/
│   │   │
│   │   ├── layouts/
│   │   ├── routes/
│   │   ├── shared/
│   │   ├── locales/
│   │   ├── types/
│   │   ├── i18n.ts
│   │   └── main.tsx
│   │
│   ├── public/
│   ├── package.json
│   └── vite.config.ts
│
└── backend/
    ├── src/
    │   ├── main/
    │   │   ├── java/
    │   │   │   └── com/example/backend/
    │   │   │       ├── config/
    │   │   │       ├── global/
    │   │   │       └── trma/
    │   │   │           ├── controller/
    │   │   │           ├── dto/
    │   │   │           ├── mapper/
    │   │   │           └── service/
    │   │   │
    │   │   └── resources/
    │   │       └── mapper/
    │   │
    │   └── test/
    │
    └── build.gradle

⸻

🔄 데이터 처리 구조

TripMate는 Frontend와 Backend를 REST API로 연결합니다.

React
  │
  │ HTTP Request
  ▼
Controller
  │
  ▼
Service
  │
  ▼
MyBatis Mapper
  │
  ▼
MySQL

외부 관광 데이터가 필요한 경우 Backend에서 외부 API를 호출합니다.

React
  │
  ▼
Spring Boot
  │
  ├── MySQL
  │
  ├── 한국관광공사 API
  │
  └── Gemini API

⸻

🧩 Backend API 구성

사용자

POST /login/existsUserId
POST /login/signup
GET  /login/login
GET  /login/userDetail
GET  /login/reviewList

홈

GET /trmaHome/userInfo
GET /trmaHome/bestTourList
GET /trmaHome/bestMoimList

여행지

GET /tourList/tourSearch
GET /tourList/tourAiSearch
GET /tourList/tourDetail
GET /tourList/tourAIDetail
GET /tourList/tourDetailReview

소모임

GET  /moimList/moimSearch
GET  /moimList/moimAiSearch
GET  /moimList/moimDetail
GET  /moimList/myMoim
GET  /moimList/moimCateSearch
POST /moimList/createMoim

관광 API

GET /tourAPI/tourInfoList

⸻

🎨 Frontend 주요 페이지

페이지	경로	설명
홈	/	인기 여행지 및 인기 소모임
여행지 목록	/placeList	여행지 검색 및 필터
여행지 상세	/place/:tourId	여행지 상세 및 리뷰
소모임 목록	/moimList	소모임 검색 및 필터
소모임 상세	/moim/:moimId	소모임 상세 정보
소모임 생성	/createMoim	소모임 생성
소모임 관리	/moimManage	내 소모임 관리
마이페이지	/my	사용자 정보 및 리뷰
가이드	/guide	서비스 이용 안내

⸻

🚀 실행 방법

1. Repository Clone

git clone <repository-url>
cd TripMate-main

⸻

2. Frontend 실행

cd frontend
npm install
npm run dev

개발 서버가 실행되면 Vite에서 제공하는 주소로 접속합니다.

환경변수는 프로젝트의 .env 파일에 설정합니다.

VITE_API_BASE_URL=<BACKEND_API_URL>

⸻

3. Backend 실행

cd backend
./gradlew bootRun

Windows 환경에서는:

gradlew.bat bootRun

Backend 기본 포트:

8080

⸻

🔐 환경 변수

외부 API Key, DB 접속 정보, JWT Secret 등의 민감한 정보는 Repository에 직접 작성하지 않습니다.

예시:

# Frontend
VITE_API_BASE_URL=
# Backend
DB_URL=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
GEMINI_API_KEY=
TOUR_API_KEY=

실제 프로젝트 실행에 필요한 값은 별도의 환경 설정을 통해 주입합니다.

⸻

📌 개발 포인트

서버 상태 관리

Frontend에서는 TanStack Query를 사용하여 서버 데이터를 관리했습니다.

Component
   │
   ▼
Custom Hook
   │
   ▼
TanStack Query
   │
   ▼
API Client
   │
   ▼
Backend API

여행지 및 소모임 목록에서는 useInfiniteQuery를 사용하여 페이지 단위 데이터를 관리하고 무한 스크롤을 구현했습니다.

공통 API Client

Fetch 기반의 공통 API Client를 구성하여 API 호출 형식을 통일했습니다.

Component
   ↓
Custom Hook
   ↓
apiClient
   ↓
fetch
   ↓
Backend

계층형 Backend 구조

Backend는 Controller / Service / Mapper 구조로 역할을 분리했습니다.

Controller
    ↓
Service
    ↓
Mapper
    ↓
Database

이를 통해 HTTP 요청 처리와 비즈니스 로직, 데이터 접근 로직을 분리했습니다.

⸻

🗂️ Git Branch

main
└── feature/*

기능 단위로 Branch를 분리하여 개발하고 작업 내용을 관리합니다.

⸻

📈 현재 개발 상태

구현

* [x]	홈 화면
* [x]	인기 여행지 조회
* [x]	인기 소모임 조회
* [x]	여행지 목록
* [x]	여행지 필터
* [x]	여행지 상세
* [x]	여행지 리뷰
* [x]	소모임 목록
* [x]	소모임 필터
* [x]	소모임 무한 스크롤
* [x]	소모임 상세
* [x]	다국어 처리
* [x]	Backend REST API
* [x]	JWT 기반 인증 구조
* [x]	관광공사 API 연동
* [x]	AI 추천 API 구조

개발 진행 중

* [ ]	회원가입 UI 연동
* [ ]	로그인 UI 연동
* [ ]	소모임 생성 전체 Step 구현
* [ ]	소모임 관리 기능
* [ ]	마이페이지 API 연동
* [ ]	AI 추천 UI
* [ ]	실제 사용자 데이터 기반 리뷰 기능
* [ ]	배포 환경 구성

⸻

👥 역할

Frontend

* React + TypeScript 기반 UI 구현
* 페이지 및 라우팅 구성
* 공통 컴포넌트 설계
* TanStack Query를 이용한 서버 상태 관리
* API Client 구성
* 여행지/소모임 목록 및 상세 화면 구현
* 무한 스크롤 구현
* 다국어 처리
* 반응형 UI 및 접근성 고려

Backend

* Spring Boot REST API 개발
* Controller / Service / Mapper 구조 설계
* MyBatis 기반 DB 연동
* JWT 인증 구조 구현
* 관광공사 API 연동
* AI API 연동
* Swagger API 문서화
* 예외 처리 및 공통 응답 구조 구현

⸻

📄 License

This project was created for educational and portfolio purposes.
