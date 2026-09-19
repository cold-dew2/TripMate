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
        },
    },
});

createRoot(document.getElementById('root')!).render(
    <QueryClientProvider client={queryClient}>
        <App />
    </QueryClientProvider>
)
