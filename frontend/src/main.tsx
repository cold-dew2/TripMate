import { createRoot } from 'react-dom/client'
import "./i18n";
import App from './App.tsx'
import "@/shared/styles/index.css";
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            // react-query는 실패한 쿼리를 기본 3번(약 1s/2s/4s 지연) 재시도한다.
            // "로그인 필요"/"AI 기능 사용 불가" 같은 건 서버가 항상 같은 응답을 주는
            // 확정적인 결과라 재시도해도 결과가 똑같은데, 기본값 그대로 두면 비로그인
            // 사용자가 페이지를 열 때마다 로그인 여부 확인(useUser 등)에만 7초 넘게
            // 걸리는 원인이 된다. 그런 경우는 즉시 포기하고, 그 외 진짜 네트워크
            // 오류만 1번 재시도한다.
            retry: (failureCount, error) => {
                const code = (error as { code?: string } | null)?.code;
                if (code === "NEED_LOGIN" || code === "AI_UNAVAILABLE") return false;
                return failureCount < 1;
            },
            // staleTime 기본값(0)이면 캐시가 있어도 "약간이라도 지난" 데이터로 취급해
            // 다시 mount될 때마다 곧바로 백그라운드 재요청을 건다. 번역 API처럼 응답이
            // 느린 화면(관광지 목록, 모임 상세 등)은 이 재요청 때문에 뒤로 갔다 다시
            // 들어올 때마다 로딩이 다시 도는 것처럼 보였다. 이 정도 데이터(관광지/모임
            // 정보)는 몇 분 안에 바뀔 일이 거의 없으니, 잠깐은 "신선하다"고 보고 재요청을
            // 건너뛴다 — 실시간성이 필요한 화면(채팅, 알림 등)은 각자 자기 훅에서
            // staleTime을 0으로 따로 지정해 이 기본값을 무시하면 된다.
            staleTime: 60 * 1000,
        },
    },
});

createRoot(document.getElementById('root')!).render(
    <QueryClientProvider client={queryClient}>
        <App />
    </QueryClientProvider>
)
