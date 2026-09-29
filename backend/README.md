# TripMate Backend

TripMate의 Backend 프로젝트입니다.

Spring Boot와 Java를 기반으로 여행지 탐색, 여행 소모임, 사용자, 리뷰, 채팅, 알림 등의 API를 구현했습니다.

프로젝트를 개발하면서 JWT 인증, MyBatis 기반 데이터 처리, 실시간 WebSocket 통신, 외부 API 연동, AI 응답 지연 및 캐싱, 예외 처리 등 실제 서비스에서 발생할 수 있는 문제를 해결하는 데 중점을 두었습니다.

---

# 🌐 배포 서비스

**TripMate 바로가기**

https://trip-mate-ten-chi.vercel.app

### 배포 환경

| 구분 | 환경 |
|---|---|
| Frontend | Vercel |
| Backend | Google Cloud Platform (GCP) |
| Database | Google Cloud Platform (GCP) |

```text
사용자
  │
  ▼
Vercel
  │
  │ REST API
  │ WebSocket / STOMP
  ▼
GCP Backend
  │
  ▼
GCP Database
```
---

# 🚀 실행 방법

### macOS / Linux

```bash
cd backend
./gradlew bootRun
```

### Windows

```bat
cd backend
gradlew.bat bootRun
```

### Build

```bash
./gradlew clean build
```

### JAR 실행

```bash
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar
```

Backend 기본 포트는 `8080`입니다.

환경 변수:

```env
DB_URL=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
GEMINI_API_KEY=
TOUR_API_KEY=
```

---

# 🧩 기술을 선택한 이유

## Spring Boot

REST API와 WebSocket을 하나의 Backend에서 함께 구성하기 위해 Spring Boot를 사용했습니다.

TripMate는 단순 CRUD뿐 아니라 인증, 외부 API 연동, 실시간 채팅, AI 요청 등 여러 종류의 서버 기능이 필요했기 때문에 Controller / Service / Mapper로 책임을 분리하기 쉬운 Spring 구조를 선택했습니다.

```text
Client
 ↓
Controller
 ↓
Service
 ↓
Mapper / External API
 ↓
Database / External Service
```

---

## MyBatis

TripMate의 데이터 조회에는 검색, 필터, 정렬, 페이징, 집계 등 SQL을 직접 제어해야 하는 경우가 많았습니다.

따라서 SQL을 명확하게 관리할 수 있는 MyBatis를 사용했습니다.

```text
Controller
    ↓
Service
    ↓
Mapper
    ↓
MyBatis XML
    ↓
MariaDB
```

특히 여행지와 소모임 목록처럼 여러 조건이 조합되는 조회에서는 SQL을 직접 작성할 수 있다는 점을 활용했습니다.

---

## Spring Security + JWT

로그인 이후 API 요청에서 사용자의 인증 상태를 확인하기 위해 Spring Security와 JWT를 사용했습니다.

```text
로그인
 ↓
사용자 인증
 ↓
JWT 생성
 ↓
Client
 ↓
API 요청
 ↓
JWT 검증
 ↓
SecurityContext
```

JWT 검증을 Filter에서 처리하여 Controller가 인증 로직을 직접 처리하지 않도록 분리했습니다.

---

## WebSocket + STOMP

채팅은 실시간으로 메시지를 전달해야 하기 때문에 WebSocket을 사용했습니다.

STOMP를 함께 사용하여 메시지 destination과 subscription을 구분했습니다.

```text
Client
 ↓ WebSocket / STOMP
Backend
 ├── 메시지 저장
 └── 구독 중인 사용자에게 전달
```

REST API는 기존 메시지 조회와 최신 데이터 재조회에 사용하고, WebSocket은 실시간 메시지 전달에 사용하는 방식으로 역할을 나누었습니다.

---

## Gemini API

여행지 검색, 일정 생성, 교통 추천, 콘텐츠 번역처럼 정해진 규칙만으로 처리하기 어려운 기능에 Gemini API를 사용했습니다.

AI API는 일반적인 DB 조회보다 응답 시간이 길고 외부 서비스 상태에 영향을 받기 때문에 반복적으로 사용할 수 있는 결과는 DB에 저장하여 캐시처럼 활용했습니다.

```text
AI 요청
 ↓
캐시 확인
 ├── HIT → 저장된 결과 반환
 │
 └── MISS
      ↓
   Gemini API
      ↓
   결과 저장
      ↓
   응답
```

---

# 🐛 주요 Troubleshooting

## 1. JWT 인증 필터 초기화 과정에서 서버가 실행되지 않는 문제

### 문제

Spring Security와 JWT 인증 필터를 연결한 뒤 애플리케이션 시작 과정에서 인증 필터 또는 사용자 조회에 필요한 Bean을 찾지 못해 서버가 정상적으로 시작되지 않는 문제가 있었습니다.

```text
Spring Boot 시작
 ↓
Security 설정 초기화
 ↓
JWT Filter 생성
 ↓
Bean 의존성 확인
 ↓
초기화 실패
```

### 해결

인증에 필요한 책임을 분리했습니다.

```text
JwtAuthenticationFilter
        ↓
JWT 추출 / 검증
        ↓
UserDetailsService
        ↓
사용자 조회
        ↓
SecurityContext
```

Security 설정에서는 인증 필터를 Filter Chain에 등록하고, 사용자 조회는 별도의 서비스가 담당하도록 구성했습니다.

### 선택 이유

JWT 검증과 사용자 조회를 하나의 클래스에서 처리하면 인증 로직이 커지고 의존 관계가 복잡해집니다.

따라서 Filter는 인증 요청의 진입과 토큰 검증을 담당하고, UserDetailsService는 사용자 조회를 담당하도록 역할을 분리했습니다.

---

## 2. 인증이 필요한 API와 공개 API의 접근 정책이 섞이는 문제

### 문제

TripMate에는 로그인 없이 접근 가능한 여행지 탐색 기능과 로그인 후 사용할 수 있는 마이페이지, 소모임 관리, 채팅 등의 기능이 함께 존재합니다.

모든 API를 동일하게 인증 처리하면 공개 기능까지 로그인을 요구하게 되고, 반대로 모두 허용하면 보호해야 할 API까지 노출될 수 있었습니다.

### 해결

Spring Security에서 URL별 접근 정책을 분리했습니다.

```text
공개 API
→ permitAll

인증 필요 API
→ authenticated

JWT
→ SecurityContext에 인증 정보 설정
```

### 선택 이유

Controller마다 로그인 여부를 직접 검사하는 방식보다 Security Layer에서 공통적으로 처리하는 편이 인증 책임을 일관되게 유지할 수 있습니다.

---

## 3. 비밀번호를 안전하게 저장하고 검증하는 문제

### 문제

회원가입 과정에서 사용자가 입력한 비밀번호를 DB에 그대로 저장하면 보안상 문제가 발생합니다.

### 해결

BCrypt 기반 PasswordEncoder를 사용하여 비밀번호를 해시한 뒤 저장하고, 로그인 시 입력값과 저장된 해시를 비교하도록 구성했습니다.

```text
회원가입
 ↓
PasswordEncoder
 ↓
BCrypt Hash
 ↓
DB 저장

로그인
 ↓
입력 비밀번호
 ↓
PasswordEncoder.matches()
 ↓
인증 성공 / 실패
```

### 선택 이유

직접 암호화 로직을 구현하지 않고 Spring Security의 PasswordEncoder를 사용하여 비밀번호 저장과 검증을 표준적인 방식으로 처리했습니다.

---

## 4. MyBatis 검색 조건이 늘어나면서 SQL 관리가 복잡해지는 문제

### 문제

여행지와 소모임 목록에는 검색어, 지역, 카테고리, 정렬, 페이징 등 여러 조건이 사용됩니다.

조건을 Java 코드에서 문자열로 조합하면 SQL 관리가 어려워지고 수정 범위가 커질 수 있었습니다.

### 해결

조회 조건과 SQL을 MyBatis Mapper에서 관리하고 필요한 조건만 동적으로 적용했습니다.

```text
Request
 ↓
DTO
 ↓
Service
 ↓
Mapper
 ↓
MyBatis Dynamic SQL
 ↓
MariaDB
```

검색 조건에 따라 필요한 SQL 조건을 조합할 수 있도록 Mapper에서 조회 로직을 관리했습니다.

### 선택 이유

검색 조건은 SQL의 조건문과 직접 연결되어 있기 때문에 SQL을 Mapper에서 관리하면 쿼리의 의도를 확인하기 쉽고 수정 범위를 줄일 수 있습니다.

---

## 5. 목록 API에서 데이터가 많아질수록 한 번에 조회하는 문제

### 문제

여행지나 소모임 목록을 전체 조회하면 데이터가 많아질수록 응답 데이터가 커지고 초기 화면 로딩에도 영향을 줄 수 있습니다.

### 해결

목록 API에 페이지 단위 조회를 적용했습니다.

```text
page / size
    ↓
MyBatis LIMIT / OFFSET
    ↓
필요한 데이터만 조회
    ↓
Frontend에서 다음 페이지 요청
```

Frontend에서는 이 API를 기반으로 무한 스크롤을 구현했습니다.

### 선택 이유

처음부터 모든 데이터를 가져오는 방식보다 화면에 필요한 데이터만 단계적으로 조회하는 방식이 네트워크와 DB 조회량을 줄이는 데 적합했습니다.

---

## 6. WebSocket 연결이 끊긴 동안 메시지가 누락될 수 있는 문제

### 문제

WebSocket 연결이 끊기면 자동 재연결이 되더라도 연결이 끊긴 동안 발생한 메시지를 클라이언트가 받지 못할 수 있습니다.

### 해결

REST API와 WebSocket의 역할을 분리했습니다.

```text
REST
→ 기존 메시지 조회
→ 최신 메시지 재조회

WebSocket / STOMP
→ 새 메시지 실시간 전달
→ 읽음 상태
```

연결이 다시 이루어진 후 REST API로 최신 메시지를 조회하여 누락된 데이터를 보완하도록 구성했습니다.

또한 메시지에는 `messageId`를 기준으로 중복 여부를 확인하여 REST 응답과 WebSocket Echo가 중복 표시되지 않도록 처리했습니다.

### 선택 이유

WebSocket은 실시간 전달에 적합하지만 연결이 항상 유지된다는 보장은 없습니다.

따라서:

```text
실시간성
→ WebSocket

데이터 정합성
→ DB + REST 조회
```

로 역할을 나누었습니다.

---

## 7. 채팅 메시지 저장과 읽음 상태를 안정적으로 관리하는 문제

### 문제

채팅에서는 메시지 자체의 데이터와 사용자가 어디까지 읽었는지를 별도로 관리해야 합니다.

메시지마다 모든 사용자의 읽음 여부를 저장하면 메시지 수와 참여자가 증가할수록 관리해야 할 데이터가 복잡해질 수 있습니다.

### 해결

메시지 저장과 사용자별 마지막 읽음 상태를 분리했습니다.

```text
ChatRoom
 ├── Message
 ├── Member A → lastRead
 ├── Member B → lastRead
 └── Member C → lastRead
```

메시지를 처리할 때 DB에 저장한 데이터를 기준으로 실시간 전달하고, 사용자의 마지막 읽음 위치를 기준으로 읽지 않은 메시지를 계산할 수 있도록 구성했습니다.

```text
Client
 ↓
STOMP 메시지
 ↓
Backend
 ↓
DB 저장
 ↓
저장된 메시지 기반 전달
 ↓
구독 중인 Client
```

### 선택 이유

채팅 화면에 표시되는 데이터와 DB에 저장된 데이터가 일치해야 이후 REST 조회에서도 동일한 메시지를 확인할 수 있습니다.

또한 읽음 상태를 메시지 데이터와 분리하여 참여자별 상태를 관리하기 쉽게 구성했습니다.

---

## 8. 외부 한국관광공사 API 데이터와 서비스 데이터를 함께 사용해야 하는 문제

### 문제

TripMate는 자체 DB에 저장된 소모임·리뷰 데이터뿐 아니라 한국관광공사에서 제공하는 관광 데이터도 사용합니다.

외부 API 응답을 그대로 Frontend에 전달하면 Frontend가 외부 API의 응답 형식에 직접 의존하게 됩니다.

### 해결

Backend에서 외부 API를 호출하고 TripMate에서 사용하는 형태로 데이터를 가공한 후 Frontend에 전달했습니다.

```text
Frontend
 ↓
TripMate API
 ↓
Backend Service
 ↓
한국관광공사 API
 ↓
응답 가공
 ↓
Frontend
```

### 선택 이유

외부 API의 응답 형식이나 변경 사항에 대한 영향을 Backend에서 흡수하고 Frontend에는 TripMate에서 사용하는 데이터 형태를 제공하기 위해 Backend를 중간 계층으로 사용했습니다.

---

## 9. Gemini API 응답 지연과 외부 서비스 장애에 대응하는 문제

### 문제

AI 기능은 일반적인 DB 조회보다 응답 시간이 길 수 있고, 외부 API 상태에 따라 요청이 실패할 수도 있습니다.

특히 일정 생성이나 번역처럼 Gemini API의 응답을 기다려야 하는 기능은 동일한 요청이 반복될 경우 불필요하게 긴 응답 시간이 발생할 수 있습니다.

### 해결

반복적으로 사용할 수 있는 AI 결과를 DB에 저장하여 캐시처럼 활용했습니다.

```text
AI 요청
 ↓
DB 캐시 조회
 ├── 캐시 존재
 │     ↓
 │   저장된 결과 반환
 │
 └── 캐시 없음
       ↓
    Gemini API
       ↓
    결과 저장
       ↓
    응답
```

이미 DB에 저장된 결과가 있는 경우 외부 API를 다시 호출하지 않고 기존 결과를 사용할 수 있도록 구성했습니다.

### 선택 이유

동일한 조건에서 재사용할 수 있는 결과까지 매번 외부 API를 호출하지 않도록 하여 응답 시간과 외부 서비스 의존성을 줄였습니다.

---

## 10. 다국어 콘텐츠와 API 오류를 일관되게 처리하는 문제

### 문제

버튼, 메뉴, 안내 문구와 같은 UI 문자열과 여행지명, 소모임명, 리뷰, 채팅 등의 동적 콘텐츠는 관리 방식이 다릅니다.

또한 API 오류를 각 Controller에서 개별적으로 처리하면 응답 형식이 달라지고 Validation 오류와 서버 내부 오류를 구분하기 어려웠습니다.

### 해결

정적인 UI 문자열은 Frontend에서 관리하고 동적인 콘텐츠는 Backend에서 번역 결과를 관리하도록 분리했습니다.

```text
정적 UI
→ i18next
→ ko / en Resource

동적 콘텐츠
→ Backend
→ 번역 결과 DB 저장
→ 필요한 언어로 조회
```

API 예외는 공통 예외 처리 계층에서 관리했습니다.

```text
Controller / Service
        ↓
Exception 발생
        ↓
Global Exception Handler
        ↓
공통 Error Response
        ↓
Frontend
```

Validation 오류와 서버 내부 오류도 구분하여 처리했습니다.

```text
잘못된 요청
→ Validation Error
→ Client 수정 필요

서버 내부 오류
→ Internal Server Error
→ 서버 문제
```

### 선택 이유

UI 번역과 동적 데이터 번역은 데이터의 성격이 다르기 때문에 관리 방식을 분리했습니다.

또한 예외 처리를 하나의 계층에서 관리하면 API 응답 형식을 일관되게 유지하고 Frontend에서 오류 원인을 구분하기 쉽습니다.

---

# 🧱 공통 Backend 구조

Backend는 기능별 Controller / Service / Mapper 구조를 기준으로 구성했습니다.

```text
Controller
→ HTTP 요청 / 응답 처리

Service
→ 비즈니스 로직

Mapper
→ DB 접근

DTO
→ 요청 / 응답 데이터 전달

Security / JWT
→ 인증 및 접근 제어

Exception
→ 공통 예외 처리

Config
→ Spring 및 외부 기능 설정
```

Controller에 DB 조회나 복잡한 비즈니스 로직이 직접 들어가지 않도록 각 계층의 책임을 분리했습니다.

---

# 🔐 인증 처리

TripMate에서는 인증 관련 책임을 다음과 같이 구분했습니다.

```text
Spring Security
→ 인증 흐름 및 접근 제어

JWT Filter
→ 요청 JWT 추출 및 검증

UserDetailsService
→ 사용자 조회

SecurityContext
→ 현재 요청의 인증 사용자 관리

Service
→ 인증된 사용자 기준의 비즈니스 로직
```

Controller가 토큰을 직접 파싱하거나 인증 로직을 수행하지 않도록 하여 비즈니스 로직과 인증 로직을 분리했습니다.

---

# 💬 실시간 채팅 처리

채팅에서는 REST와 WebSocket의 역할을 구분했습니다.

| 기능 | 방식 | 이유 |
|---|---|---|
| 채팅방 목록 | REST | 일반적인 조회 |
| 기존 메시지 조회 | REST | DB 기준 데이터 조회 |
| 최신 메시지 재조회 | REST | 연결 복구 후 데이터 보완 |
| 새 메시지 | WebSocket / STOMP | 실시간 전달 |
| 읽음 상태 | WebSocket / DB | 실시간 상태 전달 및 저장 |

```text
REST
→ 조회와 데이터 정합성

WebSocket / STOMP
→ 실시간 이벤트 전달
```

WebSocket이 일시적으로 끊겨도 REST 조회를 통해 최신 상태를 다시 확인할 수 있도록 구성했습니다.

---

# 🌐 다국어 처리

TripMate에서는 정적 UI와 동적 콘텐츠의 번역 방식을 분리했습니다.

```text
Frontend
→ UI 문자열 번역
→ i18next

Backend
→ 관광지 / 소모임 / 리뷰 / 채팅 등 동적 콘텐츠
→ 번역 결과 DB 저장
```

번역 결과는 재사용할 수 있도록 캐시하여 동일한 콘텐츠에 대한 반복적인 번역 요청을 줄였습니다.

---

# 🤖 AI 처리

AI 기능은 Gemini API를 직접 반복 호출하는 방식이 아니라 캐시 여부를 먼저 확인하도록 구성했습니다.

```text
Client
 ↓
Controller
 ↓
Service
 ↓
AI Cache 조회
 ├── HIT
 │    ↓
 │  DB 결과 반환
 │
 └── MISS
      ↓
   Gemini API
      ↓
   결과 가공
      ↓
   DB 저장
      ↓
   응답
```

AI 여행지 검색, 여행 일정 생성, 교통 추천, 콘텐츠 번역 등 반복 사용 가능한 결과를 저장하여 외부 API 호출을 줄였습니다.

---

# 🗄️ Database

Backend는 MariaDB와 MyBatis를 사용합니다.

신규 기능에 따른 테이블 및 컬럼 변경은 다음 SQL 파일에서 관리했습니다.

```text
backend/
└── src/main/resources/
    └── schema/
        └── new_features.sql
```

주요 변경 대상은 다음과 같습니다.

- 실시간 채팅
- 채팅방 멤버 및 읽음 상태
- 안심 신고
- 사용자 프로필 확장
- 후기 이미지
- 관광지 좌표
- 사용자 사용 언어
- 알림
- 소모임 대표 이미지
- 관광지 / 소모임 / 리뷰 / 채팅 번역 캐시
- AI 관광지 이용정보 캐시
- 검색 성능 개선용 인덱스

---

# 📈 추가 개선사항

## 1. API 응답 및 DTO 구조 정리

기능이 늘어날수록 요청/응답 DTO가 많아지기 때문에 API별 DTO의 역할을 더 명확하게 분리할 수 있습니다.

```text
Request DTO
→ 입력 데이터

Service
→ 비즈니스 처리

Response DTO
→ 외부 응답 데이터
```

이를 통해 DB 구조가 API 응답에 직접 노출되는 것을 줄이고 API 계약을 명확하게 유지할 수 있습니다.

---

## 2. 공통 응답 구조 강화

현재 공통 예외 처리 구조를 기반으로 성공 응답까지 동일한 규칙으로 통일하면 Frontend에서 API 응답을 더 일관되게 처리할 수 있습니다.

```text
Success
→ 공통 Success Response

Failure
→ 공통 Error Response
```

---

## 3. 외부 API 호출 계층 분리

한국관광공사 API와 Gemini API처럼 외부 서비스가 추가될 경우 외부 API 호출 코드를 별도의 Client 계층으로 더 명확하게 분리할 수 있습니다.

```text
Service
 ↓
External Client
 ├── Tourism API Client
 └── Gemini API Client
```

이를 통해 외부 서비스 변경이나 테스트 시 영향을 받는 범위를 줄일 수 있습니다.

---

## 4. AI 처리 비동기화

AI 일정 생성이나 번역처럼 응답 시간이 긴 기능은 향후 비동기 처리 또는 작업 상태 기반 API로 개선할 수 있습니다.

```text
AI 요청
 ↓
작업 생성
 ↓
처리 중
 ↓
결과 저장
 ↓
완료
```

이를 적용하면 긴 AI 응답을 하나의 HTTP 요청에서 계속 기다리는 구조를 개선할 수 있습니다.

---

## 5. 목록 조회 성능 개선

데이터가 계속 증가하는 환경에서는 OFFSET 기반 페이지네이션보다 Cursor Pagination을 적용하는 방식을 검토할 수 있습니다.

또한 검색 조건에 맞는 인덱스를 추가하여 목록 조회 성능을 개선할 수 있습니다.

---

# 🔐 환경 변수

DB 접속 정보와 외부 API Key, JWT Secret 등 민감한 값은 환경에 맞게 주입합니다.

```env
DB_URL=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
GEMINI_API_KEY=
TOUR_API_KEY=
```

실제 Secret이나 API Key는 Repository에 직접 커밋하지 않도록 관리합니다.

---

# 📂 주요 Backend 경로

```text
backend/
└── src/main/java/com/example/backend/
    ├── config/
    ├── global/
    ├── exception/
    ├── jwt/
    ├── security/
    │
    └── trma/
        ├── controller/
        ├── dto/
        ├── mapper/
        └── service/
```

주요 역할:

| 경로 | 역할 |
|---|---|
| `controller/` | REST / WebSocket 요청 처리 |
| `service/` | 비즈니스 로직 |
| `mapper/` | DB 접근 |
| `dto/` | 요청 / 응답 데이터 |
| `jwt/` | JWT 생성 및 검증 |
| `security/` | Spring Security 설정 |
| `exception/` | 공통 예외 처리 |
| `config/` | 애플리케이션 설정 |
| `resources/mapper/` | MyBatis SQL |
| `resources/schema/` | DB 변경 SQL |

---

## 📌 마무리

TripMate Backend에서는 단순히 REST API를 구현하는 것에 그치지 않고, 인증, DB 조회, 실시간 통신, 외부 API, AI, 다국어 콘텐츠 등 각각의 특성에 맞는 처리 방식을 선택했습니다.

특히 JWT 인증과 Security 책임 분리, MyBatis 기반 검색·페이징, WebSocket과 REST의 역할 분리, AI 결과 캐싱, 공통 예외 처리처럼 실제 서비스에서 발생할 수 있는 문제를 해결하면서 **기능 구현보다 안정적인 데이터 처리와 유지보수 가능한 구조를 만드는 것**에 중점을 두었습니다.
