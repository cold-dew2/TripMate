# TripMate Frontend

TripMate의 사용자 화면을 담당하는 Frontend 프로젝트입니다.

React와 TypeScript를 기반으로 여행지 탐색, 여행 소모임 탐색 및 상세 정보 확인 등의 기능을 구현했습니다.

프로젝트를 개발하면서 서버 상태 관리, 무한 스크롤, 복잡한 Form, 실시간 채팅, 다국어, AI 응답 지연 등 실제 서비스에서 발생할 수 있는 문제를 해결하는 데 중점을 두었습니다.

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

이를 일반 배열처럼 사용하면서 `filter is not a function` 오류가 발생했습니다.

### 해결

각 페이지를 하나의 배열로 합친 후 화면에 전달했습니다.

```tsx
const places = useMemo(() => {
  return data?.pages.flat() ?? [];
}, [data]);
```

다음 페이지 요청은 `IntersectionObserver`로 하단을 감지하고, 이미 요청 중인 경우 중복 요청하지 않도록 처리했습니다.

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

스크롤 위치를 직접 계산하기보다 `IntersectionObserver`가 목록 하단 감지에 적합하고, `useInfiniteQuery`가 페이지 상태와 캐시를 관리하기 때문에 두 기능을 조합했습니다.

---

## 2. API 오류와 네트워크 오류를 구분하지 않으면 불필요한 재요청이 발생하는 문제

### 문제

HTTP 오류와 Backend 서버 자체에 연결할 수 없는 상황은 서로 다른데, 모든 실패를 동일하게 처리하면 불필요한 retry가 발생하거나 사용자에게 원인을 제대로 안내하기 어려웠습니다.

```text
HTTP 401 / 403 / 500
→ Backend는 응답함

Failed to fetch
→ 서버 연결 자체가 실패
```

### 해결

React Query에서는 재시도해도 결과가 달라지지 않는 비즈니스 오류는 retry하지 않고, 일시적인 네트워크 오류만 제한적으로 재시도했습니다.

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

`fetch` 자체가 실패한 경우에는 API Client에서 별도의 네트워크 상태 이벤트를 발생시켜 UI에서 서버 연결 문제를 안내하도록 구성했습니다.

```text
apiClient
   ↓
fetch 실패
   ↓
notifyBackendUnreachable()
   ↓
React UI 안내
```

### 선택 이유

HTTP 비즈니스 오류와 네트워크 장애는 해결 방법이 다르기 때문에 요청 정책과 사용자 안내를 분리했습니다.

---

## 3. API 응답이 JSON이 아닐 때 `response.json()` 오류

### 문제

모든 API 응답을 JSON이라고 가정하면 `204 No Content`, 빈 응답, 로그아웃과 같은 요청에서 `response.json()` 오류가 발생할 수 있었습니다.

### 해결

공통 API Client에서 응답을 먼저 문자열로 확인한 뒤 JSON을 파싱하도록 변경했습니다.

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

또한 API 응답 구조가 예상과 다를 경우 Hook/API 계층에서 데이터를 정규화한 후 컴포넌트에 전달했습니다.

```tsx
const items = Array.isArray(data)
  ? data
  : [];
```

### 선택 이유

각 화면에서 응답 오류와 데이터 구조를 개별적으로 처리하면 동일한 코드가 반복되기 때문에 공통 API Client와 Hook 계층에서 책임을 분리했습니다.

---

## 4. 검색·필터 변경 후 이전 목록이 남는 문제

### 문제

여행지와 소모임 목록에서 검색어, 지역, 카테고리가 변경될 때 이전 결과가 남거나 무한 스크롤이 이전 검색 조건의 다음 페이지를 요청할 수 있었습니다.

### 해결

검색 및 필터 조건을 Query Key에 포함했습니다.

```tsx
useInfiniteQuery({
  queryKey: [
    "placeList",
    keyword,
    region,
    category,
  ],
  queryFn: ({ pageParam }) =>
    getPlaceList({
      keyword,
      region,
      category,
      page: pageParam,
    }),
});
```

```text
검색 조건 변경
      ↓
Query Key 변경
      ↓
새 Query 생성
      ↓
첫 페이지부터 조회
      ↓
무한 스크롤 재시작
```

### 선택 이유

검색 조건을 별도의 전역 상태로 관리하기보다 서버 데이터의 식별자인 Query Key에 포함하는 것이 TanStack Query의 캐싱 구조와 잘 맞는다고 판단했습니다.

---

## 5. 소모임 생성에서 DatePicker와 Form 상태가 충돌하는 문제

### 문제

소모임 생성 과정에서 `react-day-picker`와 React Hook Form을 연결하면서 `DateRange | undefined` 타입 처리와 Form 값 동기화 문제가 발생했습니다.

또한 Step 이동 시 입력값이 초기화되거나 시작일을 선택하자 종료일까지 선택되는 문제가 있었습니다.

### 해결

DatePicker의 선택 상태와 제출용 Form 상태를 분리하고 `setValue`를 통해 필요한 값만 Form에 반영했습니다.

```tsx
const handleSelect = (range: DateRange | undefined) => {
  if (!range) return;

  setValue("startDate", range.from);
  setValue("endDate", range.to);
};
```

전체 Form은 상위에서 유지하고 현재 Step은 URL Query String으로 관리했습니다.

```text
/createMoim?step=1
        ↓
/createMoim?step=2
        ↓
/createMoim?step=3
```

시작일과 종료일이 모두 선택된 경우에만 달력을 닫도록 처리했습니다.

### 선택 이유

Step은 화면 상태이고 입력 데이터는 하나의 Form 데이터이므로 두 책임을 분리하는 것이 유지보수에 유리했습니다.

---

## 6. WebSocket 연결이 끊긴 동안 채팅 메시지가 누락되는 문제

### 문제

WebSocket이 끊겼다가 재연결되더라도 연결이 끊긴 동안 발생한 메시지를 놓칠 수 있었습니다.

### 해결

WebSocket은 실시간 전달에 사용하고, 재연결 후 REST API로 최신 메시지를 다시 조회하도록 구성했습니다.

```text
WebSocket 연결
      ↓
실시간 메시지 수신

연결 끊김
      ↓
자동 재연결

재연결 성공
      ↓
REST API로 최신 메시지 조회
      ↓
누락 메시지 보완
```

사용자가 메시지를 보낸 경우에도 REST 응답과 WebSocket Echo가 중복 표시되지 않도록 `messageId` 기준으로 중복을 방지했습니다.

### 선택 이유

WebSocket만으로 데이터 정합성을 보장하기보다 REST를 조회의 기준으로 두고 WebSocket을 실시간 전달 채널로 사용하는 것이 안정적이라고 판단했습니다.

---

## 7. 이미지 로딩 실패로 목록 UI가 깨지는 문제

### 문제

여행지나 소모임의 외부 이미지 URL이 존재하지 않거나 이미지 서버 응답이 실패하면 카드 이미지 영역이 깨지고 목록 정렬에도 영향을 줄 수 있었습니다.

### 해결

이미지 로딩 실패 시 fallback 이미지를 사용했습니다.

```tsx
const handleImageError = (
  e: React.SyntheticEvent<HTMLImageElement>
) => {
  e.currentTarget.src = fallbackImage;
};
```

이미지가 없는 데이터에도 동일한 이미지 영역을 유지하도록 구성했습니다.

### 선택 이유

모든 이미지 URL을 API 호출 단계에서 검증하기보다 실제 렌더링 단계에서 실패를 감지하고 대체하는 것이 프론트의 책임에 적합하다고 판단했습니다.

---

## 8. AI 응답을 기존 서비스 UI와 연결하는 문제

### 문제

AI 여행지 검색이나 추천 결과를 단순 텍스트로 표시하면 기존 여행지 탐색 기능과 분리되어 사용자가 다시 검색해야 하는 문제가 있었습니다.

### 해결

AI 응답을 서비스의 여행지 데이터 형태로 변환하고 기존 카드 UI와 연결했습니다.

```text
사용자 질문
    ↓
AI API
    ↓
AI 추천 결과
    ↓
여행지 데이터 변환
    ↓
Card UI
    ↓
상세 페이지
```

메시지 타입도 추천 데이터를 구분할 수 있도록 구성했습니다.

```tsx
type MessageType =
  | "user"
  | "bot"
  | "products";
```

### 선택 이유

AI를 별도의 기능으로 분리하기보다 기존 여행지 탐색 경험 안에서 바로 활용할 수 있도록 연결했습니다.

---

## 9. 다국어 변경 후 이전 언어의 데이터가 표시되는 문제

### 문제

UI 문자열은 `i18next`로 변경할 수 있지만 여행지명, 소모임명, 리뷰 등의 동적 데이터는 Backend 번역 결과와 함께 관리해야 했습니다.

언어만 변경하고 기존 Query Cache를 그대로 사용하면 이전 언어의 데이터가 남을 수 있었습니다.

### 해결

현재 언어를 Query Key에 포함했습니다.

```tsx
const { i18n } = useTranslation();

useQuery({
  queryKey: [
    "placeDetail",
    placeId,
    i18n.language,
  ],
  queryFn: () =>
    getPlaceDetail(
      placeId,
      i18n.language
    ),
});
```

```text
한국어
["placeDetail", "P001", "ko"]

      ↓ 언어 변경

영어
["placeDetail", "P001", "en"]
```

또한 HTML의 `lang` 속성도 현재 언어에 맞게 변경했습니다.

### 선택 이유

언어별 서버 데이터를 별도의 전역 상태로 복제하기보다 Query Key로 분리하여 캐시 구조를 명확하게 유지했습니다.

---

## 10. API 데이터 구조가 예상과 다를 때 화면이 깨지는 문제

### 문제

API가 정상적으로 응답하더라도 실제 데이터가 배열이 아닐 경우 `map`, `filter` 등의 배열 메서드에서 런타임 오류가 발생할 수 있었습니다.

### 해결

API Client 또는 Custom Hook에서 화면에 필요한 형태로 데이터를 정리한 후 컴포넌트에 전달했습니다.

```tsx
const items = Array.isArray(data)
  ? data
  : [];
```

무한 스크롤의 페이지 병합도 목록 Hook에서 담당하도록 구성했습니다.

```tsx
const items = data?.pages.flat() ?? [];
```

### 선택 이유

각 컴포넌트에서 API 응답을 개별적으로 검증하면 동일한 로직이 반복되기 때문에 데이터 가공 책임을 Hook/API 계층에 두었습니다.

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

## 📌 마무리

TripMate Frontend에서는 단순히 화면을 구현하는 것에 그치지 않고, 서버 데이터와 UI 상태를 분리하고, 비동기 요청과 실시간 데이터의 특성을 고려하여 각 기능에 맞는 처리 방식을 선택했습니다.

특히 무한 스크롤, Form Step 이동, WebSocket 재연결, AI 응답 지연, 다국어 데이터 동기화처럼 실제 서비스에서 발생할 수 있는 문제를 직접 해결하면서 **기능 구현보다 안정적인 사용자 경험과 유지보수 가능한 구조를 만드는 것**에 중점을 두었습니다.
