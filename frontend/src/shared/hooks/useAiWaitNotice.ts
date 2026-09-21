import { useEffect } from 'react';
import { useAiWait } from '@/shared/contexts/AiWaitContext';

const WAIT_DELAY_MS = 2000;

// isLoading이 2초 넘게 유지되면(=AI 응답이 오래 걸리면) 화면 상단에 대기 안내
// 팝업을 띄우고, isLoading이 꺼지면 즉시 감춘다. AI를 호출하는 화면마다 이 훅
// 하나만 붙이면 동일한 안내가 뜨도록 공통화했다.
const useAiWaitNotice = (isLoading: boolean, message?: string) => {
  const { showAiWait, hideAiWait } = useAiWait();

  useEffect(() => {
    if (!isLoading) {
      return;
    }
    const timer = window.setTimeout(() => showAiWait(message), WAIT_DELAY_MS);
    return () => {
      window.clearTimeout(timer);
      hideAiWait();
    };
  }, [isLoading, message, showAiWait, hideAiWait]);
};

export default useAiWaitNotice;
