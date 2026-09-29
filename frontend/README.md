# TripMate Frontend

TripMate의 사용자 화면을 담당하는 Frontend 프로젝트입니다.

React와 TypeScript를 기반으로 여행지 탐색, 여행 소모임 탐색 및 상세 정보 확인 등의 기능을 구현했습니다.

프로젝트를 개발하면서 서버 상태 관리, 무한 스크롤, 복잡한 Form, 실시간 채팅, 다국어, AI 응답 지연 등 실제 서비스에서 발생할 수 있는 문제를 해결하는 데 중점을 두었습니다.

---

## 📌 주요 기능

### 🏠 Home

- 인기 여행지 조회
- 인기 소모임 조회
- 여행 테마 카테고리
- 주요 페이지 바로가기

### 📍 Place

- 여행지 목록 및 검색
- 지역별 필터링
- 무한 스크롤
- 여행지 상세 정보
- 이용 정보
- 리뷰 및 평점

### 👥 Moim

- 소모임 목록 및 검색
- 지역 및 조건별 필터링
- 무한 스크롤
- 소모임 상세 정보
- 소모임 생성 Step UI
- 소모임 관리

### 👤 My

- 사용자 프로필
- 사용자 활동 정보
- 리뷰 목록
- 마이페이지

### 💬 Chat

- 채팅방 목록
- 실시간 메시지
- 읽음 상태
- WebSocket / STOMP 기반 통신

### 🌐 i18n

- 한국어 / 영어 지원
- `i18next`, `react-i18next` 사용
- 동적 콘텐츠의 언어 변경 대응

### 🤖 AI

- AI 여행지 검색
- AI 여행지 정보
- AI 소모임 검색
- AI 기반 여행 일정 / 추천 기능 연동

---

# 🧩 기술을 선택한 이유

## TanStack Query

TripMate에서 여행지, 소모임, 리뷰 등 서버에서 관리되는 데이터가 많았기 때문에 `useEffect + useState`만으로 서버 상태를 관리하지 않고 TanStack Query를 사용했습니다.

```text
Server State
→ TanStack Query

Client / UI State
→ React State

URL로 유지할 상태
→ URL Search Params
```

특히 목록 조회에서는 `useInfiniteQuery`를 사용하여 페이지 단위 데이터와 캐시를 관리했습니다.

또한 모든 요청을 무조건 재시도하지 않고, 로그인 필요나 AI 사용 불가처럼 재시도해도 결과가 달라지지 않는 비즈니스 오류는 재시도하지 않도록 처리했습니다.

---

## React Hook Form

소모임 생성은 여러 Step에서 많은 입력값을 관리해야 하기 때문에 각각의 Step에서 별도로 상태를 관리하지 않고 상위 Form에서 전체 상태를 관리했습니다.

```text
MoimCreate
 ├── Step 1
 ├── Step 2
 ├── Step 3
 ├── Step 4
 └── ...
```

Step이 변경되어도 Form 데이터가 유지되도록 구성했으며, 현재 Step은 URL Query String으로 관리했습니다.

```text
/createMoim?step=1
        ↓
/createMoim?step=2
        ↓
/createMoim?step=3
```

이를 통해 새로고침이나 브라우저 이동 시에도 현재 Step을 URL에서 확인할 수 있도록 했습니다.

---

## REST API + WebSocket / STOMP

채팅에서는 REST API와 WebSocket을 각각 다른 목적으로 사용했습니다.

```text
REST
→ 채팅방 목록
→ 기존 메시지 조회
→ 최신 데이터 재조회

WebSocket / STOMP
→ 새 메시지
→ 읽음 상태
→ 번역 결과
```

WebSocket이 끊겼을 때는 자동 재연결만 기다리지 않고 REST API로 최신 메시지를 다시 조회하여 연결이 끊긴 동안 발생한 데이터를 보완했습니다.

---

## i18next

정적인 UI 문자열은 `i18next`로 관리했습니다.

```text
locales/
├── ko/
│   └── translation.json
└── en/
    └── translation.json
```

동적 데이터는 Backend의 번역 결과와 연동하고, 현재 언어를 Query Key에 포함하여 언어 변경 후 이전 언어의 캐시가 그대로 표시되는 문제를 방지했습니다.

또한 현재 언어에 맞게 HTML의 `lang` 속성도 변경하여 스크린리더가 문서 언어를 올바르게 인식할 수 있도록 했습니다.

---

# 🐛 주요 Troubleshooting

## 1. 무한 스크롤 데이터에서 `filter is not a function`

### 문제

`useInfiniteQuery`의 데이터는 일반 배열이 아니라 페이지 배열 형태로 반환됩니다.

```text
{
  pages: [
    [...],
    [...],
    [...]
  ]
}
```

이를 일반 배열처럼 사용하면서 필터링 과정에서 `filter is not a function` 오류가 발생했습니다.

### 해결

각 페이지의 데이터를 하나의 배열로 합친 후 화면에 전달했습니다.

```tsx
const places = useMemo(() => {
  return data?.pages.flat() ?? [];
}, [data]);
```

그리고 다음 페이지 요청은 `IntersectionObserver`로 목록 하단을 감지하고, 이미 요청 중인 경우 중복 요청하지 않도록 처리했습니다.

```tsx
if (
  target.isIntersecting &&
  hasNextPage &&
  !isFetchingNextPage
) {
  fetchNextPage();
}
```

### 선택 이유

스크롤 위치를 직접 계산하는 방식보다 `IntersectionObserver`가 목록 하단 감지라는 목적에 적합하고, `useInfiniteQuery`가 페이지 상태와 캐시를 관리하기 때문에 두 기능을 조합했습니다.

---


## 3. API 실패 시 요청이 불필요하게 반복되는 문제

### 문제

TanStack Query의 기본 retry 동작 때문에 로그인하지 않은 사용자가 접근할 때와 같이 재시도해도 결과가 바뀌지 않는 요청까지 반복되는 문제가 있었습니다.

### 해결

에러 종류에 따라 재시도 여부를 구분했습니다.

```tsx
retry: (failureCount, error) => {
  const code = (error as { code?: string } | null)?.code;

  if (
    code === "NEED_LOGIN" ||
    code === "AI_UNAVAILABLE"
  ) {
    return false;
  }

  return failureCount < 1;
}
```

### 선택 이유

모든 오류를 동일하게 처리하면 비즈니스 오류에도 불필요한 재요청이 발생합니다.

따라서:

```text
비즈니스 오류
→ 재시도하지 않음

일시적인 네트워크 오류
→ 1회 재시도
```

로 구분했습니다.

---

## 4. API 응답이 JSON이 아닐 때 `response.json()` 오류

### 문제

모든 API 응답을 JSON이라고 가정하고 `response.json()`을 바로 호출하면 다음 상황에서 오류가 발생할 수 있었습니다.

- 204 No Content
- 빈 응답
- JSON이 아닌 응답
- 로그아웃처럼 Body가 없는 응답

### 해결

공통 API Client에서 먼저 응답을 문자열로 확인한 후 JSON을 파싱하도록 변경했습니다.

```text
Response
 ↓
text()
 ↓
빈 응답?
 ├─ Yes → 성공 처리
 └─ No
     ↓
JSON.parse()
```

### 선택 이유

각 API에서 개별적으로 예외를 처리하지 않고 공통 API Client에서 처리하여 모든 화면에서 동일한 응답 처리 규칙을 사용할 수 있도록 했습니다.

---

## 5. Backend 서버 자체가 내려간 경우의 오류 처리

### 문제

HTTP 401, 500과 같이 서버가 응답하는 오류와 서버 자체에 연결할 수 없는 상황은 서로 다른 문제입니다.

```text
HTTP 401
→ 서버 연결 성공 + 인증 문제

fetch 실패
→ 서버 연결 / 네트워크 문제 가능성
```

### 해결

`apiClient`에서 `fetch` 자체가 실패하면 별도의 네트워크 상태 이벤트를 발생시키고 React 화면에서 사용자에게 서버 연결 문제를 안내하도록 구성했습니다.

```text
apiClient
 ↓
fetch 실패
 ↓
notifyBackendUnreachable()
 ↓
React UI에서 안내
```

### 선택 이유

API Client는 React 컴포넌트가 아니므로 UI Hook을 직접 사용할 수 없습니다.

따라서 API 계층은 이벤트만 발생시키고 React 계층에서 이를 구독하도록 분리했습니다.

---

## 6. DateRangePicker에서 시작일을 선택하자마자 종료일까지 선택되는 문제

### 문제

`react-day-picker`의 Range Mode는 첫 번째 날짜 선택 시 내부적으로 하나의 Range가 완성된 형태로 전달될 수 있었습니다.

소모임 생성에서는:

```text
시작일 선택
 ↓
종료일 선택
```

순서가 필요했습니다.

### 해결

첫 번째 선택에서는 `to` 값을 비우고, 시작일과 종료일이 모두 선택됐을 때만 캘린더를 닫도록 처리했습니다.

```tsx
const isFirstPick = !dateRange?.from;

const nextRange =
  isFirstPick && range?.from
    ? {
        from: range.from,
        to: undefined,
      }
    : range;

if (nextRange?.from && nextRange?.to) {
  setIsOpen(false);
}
```

### 선택 이유

날짜 범위 계산 자체는 라이브러리의 기능을 사용하면서, TripMate에 필요한 사용자 경험만 추가로 보정했습니다.

---

## 7. 소모임 생성 Step 이동 시 입력값이 사라지는 문제

### 문제

소모임 생성 Step을 URL Query String으로 변경하면서 `location.state`에 있던 초기 데이터가 사라지는 문제가 있었습니다.

```text
Step 1
 ↓
URL 변경
 ↓
Step 2
 ↓
location.state 초기화
```

### 해결

페이지 최초 진입 시 `location.state`를 상위 컴포넌트의 State에 저장하고 이후 Step 변경에서는 저장된 값을 사용했습니다.

```tsx
const [prefill] = useState(
  () => location.state as MoimCreatePrefill | null
);
```

### 선택 이유

Step 변경과 초기 데이터 전달을 분리하여 URL 이동이 Form 데이터에 영향을 주지 않도록 했습니다.

---

## 8. WebSocket 연결이 끊긴 동안 메시지가 누락될 수 있는 문제

### 문제

WebSocket 연결이 끊기면 자동 재연결이 되더라도 연결이 끊긴 동안 발생한 메시지를 놓칠 수 있습니다.

### 해결

```text
WebSocket 연결
→ 실시간 메시지

연결 끊김
→ 자동 재연결

재연결 성공
→ REST로 최신 메시지 재조회
```

또한 사용자가 메시지를 전송한 경우 REST API 응답을 이용해 자신의 메시지를 즉시 화면에 반영하고, WebSocket Echo가 도착하면 `messageId`를 기준으로 중복을 방지했습니다.

```text
메시지 전송
 ↓
REST 성공
 ↓
내 화면에 즉시 표시
 ↓
WebSocket Echo
 ↓
messageId 중복 검사
```

### 선택 이유

WebSocket을 실시간 통신에 사용하면서도 데이터 정합성은 REST 조회를 통해 보완하는 방식으로 구성했습니다.

---

# 🧱 공통 컴포넌트화

반복적으로 사용되는 UI와 상태 처리는 `shared/components`에서 관리했습니다.

```text
shared/components/
├── Input
├── Button
├── Card
├── Checkbox
├── DatePicker
├── FilterTabs
├── MoimCard
├── MoimList
├── ReviewsList
├── Select
├── Skeleton
├── SpotCard
└── Tab
```

특히 목록 화면의 Loading / Error / Empty 상태는 공통화했습니다.

```tsx
<AsyncList
  isLoading={isLoading}
  isError={isError}
  data={places}
  renderSkeleton={() => <SpotCard loading />}
  renderItem={(place) => (
    // ...
  )}
/>
```

공통 컴포넌트는 단순히 UI를 재사용하기 위한 목적뿐 아니라 **동일한 상태를 여러 화면에서 일관되게 처리하기 위한 목적**으로 구성했습니다.

---

# 🔄 상태 관리 기준

프로젝트에서는 상태의 성격에 따라 관리 방법을 구분했습니다.

| 상태 | 관리 방법 | 이유 |
|---|---|---|
| 여행지 / 소모임 / 리뷰 | TanStack Query | 서버 데이터 |
| 검색어 / Modal / 선택 상태 | React State | 화면 전용 상태 |
| 목록 필터 | URL Search Params | 새로고침 및 공유 |
| 소모임 생성 Form | React Hook Form | 복잡한 Form 상태 |
| 실시간 채팅 | WebSocket + React State | 실시간 데이터 |
| 다국어 | i18next | UI Translation |

이 기준을 적용하여 하나의 상태 관리 방법으로 모든 데이터를 처리하지 않도록 했습니다.

---

# 🌐 다국어 처리

UI 문자열은 Translation Resource로 분리했습니다.

```text
src/locales/
├── ko/
│   └── translation.json
└── en/
    └── translation.json
```

```tsx
const { t } = useTranslation();

return <p>{t("place.listTitle")}</p>;
```

언어 변경 시:

```text
i18next language 변경
        ↓
HTML lang 변경
        ↓
Query Key의 언어 변경
        ↓
새 언어의 서버 데이터 요청
```

으로 처리했습니다.

---

# 🎨 접근성 고려

기존 웹 퍼블리싱 경험을 기반으로 Frontend 구현에서도 접근성을 고려했습니다.

- `label`과 입력 요소 연결
- `aria-label`
- `aria-invalid`
- `aria-describedby`
- `role="status"`
- 키보드 접근 고려
- HTML `lang` 동기화
- 오류 메시지와 입력 필드 연결
- 버튼 / 입력 요소의 의미가 명확하도록 구성

특히 동적인 상태 메시지는 `role="status"` 등을 활용하여 스크린리더에서도 상태 변화를 인지할 수 있도록 구성했습니다.

---

# 📈 추가 개선사항

## 1. API 타입 관리 강화

현재 Generic 기반 API Client를 사용하고 있지만 API별 Request / Response 타입을 더 명확하게 분리할 수 있습니다.

```text
placeApi
moimApi
chatApi
userApi
```

와 같이 API 영역을 구분하고 타입을 함께 관리하면 API 계약을 보다 명확하게 유지할 수 있습니다.

---

## 2. Query Key Factory 도입

현재 여러 Hook에서 Query Key를 배열로 직접 작성하고 있습니다.

```tsx
["placeList", category, keyword, region]
```

프로젝트 규모가 커질 경우 Query Key Factory를 도입하여 캐시 무효화와 Query Key 관리를 일관되게 만들 수 있습니다.

---

## 3. WebSocket 연결 관리 공통화

현재 채팅 기능을 중심으로 WebSocket을 사용하고 있습니다.

향후 알림 등 실시간 기능이 확대되면:

```text
WebSocketProvider
        ↓
STOMP Connection
        ↓
Chat / Notification
```

구조로 공통 연결을 관리할 수 있습니다.

---

## 4. 이미지 및 목록 성능 개선

목록 데이터가 많아질 경우 다음 기능을 추가로 적용할 수 있습니다.

- 이미지 Lazy Loading
- WebP / AVIF
- CDN
- Virtualized List
- Cursor Pagination
- 오래된 Infinite Query Page 제거

특히 긴 목록에서는 Virtualization을 적용하여 DOM에 동시에 렌더링되는 요소를 줄일 수 있습니다.

---

## 5. AI 응답 UX 개선

현재 AI 처리 중 상태와 번역 결과 재반영 구조를 사용하고 있습니다.

향후에는:

```text
AI 요청
 ↓
진행 상태
 ↓
부분 결과
 ↓
최종 결과
```

형태의 Progressive UI를 적용하고, AI 요청 실패 시 일반 검색이나 기존 데이터로 전환하는 Fallback을 추가할 수 있습니다.

---


# 🚀 실행 방법

```bash
npm install
```

### 개발 서버

```bash
npm run dev
```

### 빌드

```bash
npm run build
```

### Lint

```bash
npm run lint
```

### Production Preview

```bash
npm run preview
```

환경 변수:

```env
VITE_API_BASE_URL=<BACKEND_API_URL>
VITE_KAKAO_MAP_KEY=<KAKAO_MAP_API_KEY>
```

---

# 📂 주요 경로

| 경로 | 화면 |
|---|---|
| `/` | 홈 |
| `/placeList` | 여행지 목록 |
| `/place/:tourId` | 여행지 상세 |
| `/moimList` | 소모임 목록 |
| `/moim/:moimId` | 소모임 상세 |
| `/createMoim` | 소모임 생성 |
| `/moimManage` | 소모임 관리 |
| `/my` | 마이페이지 |
| `/guide` | 이용 가이드 |
| `/chat` | 채팅 목록 |
| `/chat/:roomId` | 채팅방 |

---

## 📌 마무리

TripMate Frontend에서는 단순히 화면을 구현하는 것에 그치지 않고, 서버 데이터와 UI 상태를 분리하고, 비동기 요청과 실시간 데이터의 특성을 고려하여 각 기능에 맞는 처리 방식을 선택했습니다.

특히 무한 스크롤, Form Step 이동, WebSocket 재연결, AI 응답 지연, 다국어 데이터 동기화처럼 실제 서비스에서 발생할 수 있는 문제를 직접 해결하면서 **기능 구현보다 안정적인 사용자 경험과 유지보수 가능한 구조를 만드는 것**에 중점을 두었습니다.
