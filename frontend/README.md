TripMate Frontend

TripMate의 사용자 화면을 담당하는 Frontend 프로젝트입니다.

React와 TypeScript를 기반으로 여행지 탐색, 여행 소모임 탐색 및 상세 정보 확인 등의 기능을 구현했습니다.

⸻

🛠️ Tech Stack

기술	버전 / 사용 목적
React	19
TypeScript	6
Vite	8
React Router	7
TanStack Query	5
React Hook Form	7
i18next	다국어 처리
CSS	스타일링

⸻

📌 주요 기능

🏠 Home

* 인기 여행지 조회
* 인기 소모임 조회
* 여행 테마 카테고리
* 주요 페이지 바로가기

📍 Place

* 여행지 목록 조회
* 지역별 필터링
* 여행지 검색
* 무한 스크롤
* 여행지 상세 정보
* 이용 정보
* 리뷰 조회
* 평점 표시

👥 Moim

* 소모임 목록 조회
* 지역별 필터링
* 소모임 검색
* 무한 스크롤
* 소모임 상세 정보
* 소모임 생성 Step UI
* 내 소모임 관리 페이지 구조

👤 My

* 사용자 프로필
* 사용자 활동 정보
* 언어 정보
* 리뷰 목록
* 마이페이지 메뉴

🌐 i18n

한국어와 영어를 지원합니다.

src/
└── locales/
    ├── ko/
    │   └── translation.json
    └── en/
        └── translation.json

react-i18next를 사용하여 컴포넌트에서 번역 데이터를 사용할 수 있도록 구성했습니다.

const { t } = useTranslation();
return <p>{t("place.listTitle")}</p>;

⸻

🏗️ 프로젝트 구조

src/
│
├── assets/
│   └── fonts/
│
├── features/
│   ├── auth/
│   │   └── pages/
│   │
│   ├── guide/
│   │   └── pages/
│   │
│   ├── home/
│   │   ├── hooks/
│   │   └── pages/
│   │
│   ├── moim/
│   │   ├── hooks/
│   │   └── pages/
│   │       ├── moimCreate/
│   │       ├── moimDetail/
│   │       ├── moimList/
│   │       └── moimManage/
│   │
│   ├── my/
│   │   ├── api/
│   │   └── pages/
│   │
│   └── place/
│       ├── hooks/
│       └── pages/
│           ├── placeDetail/
│           └── placeList/
│
├── layouts/
│   ├── MainLayout.tsx
│   ├── ContentLayout.tsx
│   └── components/
│
├── routes/
│   ├── Router.tsx
│   └── path/
│       └── paths.ts
│
├── shared/
│   ├── api/
│   ├── components/
│   ├── hooks/
│   └── styles/
│
├── locales/
│   ├── ko/
│   └── en/
│
├── types/
│
├── i18n.ts
├── App.tsx
└── main.tsx

⸻

🧱 Feature 기반 구조

기능별로 features를 분리했습니다.

features
├── home
├── place
├── moim
├── my
├── auth
└── guide

각 기능에서 필요한 Page와 Hook을 가까운 위치에 배치하여 기능 단위로 코드를 관리할 수 있도록 구성했습니다.

예:

features/place/
├── hooks/
│   ├── usePlaceDetail.ts
│   ├── usePlaceList.ts
│   ├── useReveiws.ts
│   └── useSearchPlace.ts
│
└── pages/
    ├── placeList/
    └── placeDetail/

⸻

🧩 Shared Components

여러 Feature에서 공통으로 사용할 수 있는 UI는 shared/components에서 관리합니다.

shared/components/
├── Input/
├── Button/
├── Card/
├── Checkbox/
├── DatePicker/
├── FilterTabs/
├── MoimCard/
├── MoimList/
├── ReviewsList/
├── Select/
├── Skeleton/
├── SpotCard/
└── Tab/

예를 들어 여행지와 소모임 목록에서 사용하는 로딩/에러/빈 데이터 처리는 AsyncList를 통해 공통화했습니다.

<AsyncList
  isLoading={isLoading}
  isError={isError}
  data={places}
  renderSkeleton={() => <SpotCard loading />}
  renderItem={(place) => (
    // ...
  )}
/>

⸻

🔄 API 통신 구조

Fetch 기반의 공통 API Client를 구성했습니다.

Page
 │
 ▼
Custom Hook
 │
 ▼
apiClient
 │
 ▼
fetch
 │
 ▼
Spring Boot API

API Client

src/shared/api/client.ts

API 응답은 성공/실패를 구분할 수 있도록 공통 타입으로 관리합니다.

interface ApiSuccess<T> {
  success: true;
  data: T;
}
interface ApiFailure {
  success: false;
  status: number;
  message: string;
}

⸻

⚡ TanStack Query

서버에서 받아오는 데이터는 TanStack Query를 사용하여 관리합니다.

일반 조회

useQuery({
  queryKey: ["placeDetail", tourId],
  queryFn: fetchPlaceDetail,
});

무한 스크롤

여행지와 소모임 목록은 useInfiniteQuery를 사용합니다.

Page 1
 ↓
Page 2
 ↓
Page 3
 ↓
Page 4

페이지 하단의 Intersection Observer가 다음 페이지 요청을 트리거합니다.

사용자가 목록 하단 접근
        ↓
IntersectionObserver
        ↓
fetchNextPage()
        ↓
TanStack Query
        ↓
다음 페이지 데이터 요청
        ↓
기존 데이터 + 신규 데이터

이를 통해 한 번에 많은 데이터를 렌더링하지 않고 필요한 시점에 데이터를 추가로 가져오도록 구성했습니다.

⸻

🧭 Routing

React Router의 createBrowserRouter를 사용하여 라우팅을 구성했습니다.

Router
│
├── MainLayout
│   └── /
│
├── ContentLayout
│   ├── /moimList
│   ├── /placeList
│   ├── /moim/:moimId
│   ├── /moimManage
│   └── /place/:tourId
│
└── No Layout
    ├── /my
    └── /createMoim

페이지 성격에 따라 Layout을 분리하여 공통 Header, Navigation 등의 UI를 재사용할 수 있도록 했습니다.

⸻

🧱 Layout 구조

MainLayout
 └── Home
ContentLayout
 ├── PageHeader
 └── Content
No Layout
 ├── MyPage
 └── MoimCreate

Layout과 Page의 책임을 분리하여 페이지별 UI 구성을 단순화했습니다.

⸻

📝 소모임 생성 Form

소모임 생성 화면은 react-hook-form을 사용하여 Form 상태를 관리합니다.

현재 생성 화면은 Step 기반으로 구성되어 있습니다.

/createMoim?step=1
        ↓
/createMoim?step=2
        ↓
/createMoim?step=3
        ↓
...

현재 Step 정보는 URL Query String으로 관리하여 새로고침이나 브라우저 이동 시 현재 단계를 URL에서 확인할 수 있도록 구성했습니다.

⸻

🎨 스타일 구조

공통 스타일과 디자인 관련 스타일을 분리했습니다.

shared/styles/
├── common.css
├── font.css
├── globals.css
├── index.css
├── reset.css
└── variables.css

공통 스타일

* Reset
* Font
* Global
* 공통 레이아웃
* CSS 변수

Font

프로젝트에서는 Pretendard 폰트를 사용합니다.

src/assets/fonts/
├── Pretendard-Regular.woff2
├── Pretendard-Medium.woff2
├── Pretendard-SemiBold.woff2
├── Pretendard-Bold.woff2
└── ...

⸻

🌐 환경 변수

API 서버 주소는 Vite 환경 변수를 통해 관리합니다.

.env

VITE_API_BASE_URL=<BACKEND_API_URL>

코드에서는 다음과 같이 사용합니다.

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

API 주소와 같은 환경별 설정값을 소스 코드와 분리하여 개발/배포 환경에 따라 변경할 수 있도록 구성했습니다.

⸻

🚀 실행 방법

설치

npm install

개발 서버

npm run dev

빌드

npm run build

Lint

npm run lint

Production Preview

npm run preview

⸻

📂 주요 경로

경로	화면
/	홈
/placeList	여행지 목록
/place/:tourId	여행지 상세
/moimList	소모임 목록
/moim/:moimId	소모임 상세
/createMoim	소모임 생성
/moimManage	소모임 관리
/my	마이페이지
/guide	이용 가이드

⸻

🔍 구현 포인트

1. Feature 단위 코드 분리

페이지 종류별로 Feature를 분리하여 기능 단위의 응집도를 높였습니다.

features/
├── place/
├── moim/
├── home/
└── my/

2. 서버 상태와 UI 상태 분리

TanStack Query를 통해 서버 데이터를 관리하고 React State는 검색어, 선택된 필터, UI 상태 등 클라이언트 상태에 집중하도록 구성했습니다.

Server State
→ TanStack Query
Client/UI State
→ useState / useRef / useEffect

3. 공통 컴포넌트화

Input, Button, Card, FilterTabs, Skeleton, Tab 등의 반복 UI를 공통 컴포넌트로 구성했습니다.

4. 무한 스크롤

useInfiniteQuery와 IntersectionObserver를 조합하여 여행지 및 소모임 목록의 무한 스크롤을 구현했습니다.

5. 다국어 지원

문자열을 코드에 직접 작성하는 대신 Translation Resource를 분리하여 한국어와 영어를 지원하도록 구성했습니다.

⸻

📈 개발 예정

* [ ]	로그인 UI 및 API 연동
* [ ]	회원가입 UI 및 API 연동
* [ ]	소모임 생성 전체 Step 구현
* [ ]	소모임 생성 API 연동
* [ ]	소모임 관리 기능
* [ ]	마이페이지 API 연동
* [ ]	AI 추천 화면 구현
* [ ]	검색 기능 고도화
* [ ]	접근성 개선
* [ ]	반응형 UI 개선
