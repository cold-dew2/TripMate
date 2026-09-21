import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import AiWaitPopup from '@/shared/components/aiWaitPopup/AiWaitPopup';

interface AiWaitContextValue {
  showAiWait: (message?: string) => void;
  hideAiWait: () => void;
}

const AiWaitContext = createContext<AiWaitContextValue | null>(null);

// AI 응답(Gemini 등)은 몇 초씩 걸릴 수 있어, 화면이 멈춘 것처럼 보이지 않도록
// 앱 전체에서 공통으로 "잠시만 기다려주세요" 안내를 띄우기 위한 컨텍스트.
// AlertContext의 확인 모달과 달리 사용자 조작을 막지 않는 비침습적 안내이며,
// AI 응답이 오면 자동으로 사라진다.
export const AiWaitProvider = ({ children }: { children: ReactNode }) => {
  const { t } = useTranslation();
  const [message, setMessage] = useState<string | null>(null);

  const showAiWait = useCallback((msg?: string) => {
    setMessage(msg ?? t('common.aiWaitNotice'));
  }, [t]);

  const hideAiWait = useCallback(() => setMessage(null), []);

  const value = useMemo(() => ({ showAiWait, hideAiWait }), [showAiWait, hideAiWait]);

  return (
    <AiWaitContext.Provider value={value}>
      {children}
      {message && <AiWaitPopup message={message} />}
    </AiWaitContext.Provider>
  );
};

export const useAiWait = () => {
  const context = useContext(AiWaitContext);
  if (!context) {
    throw new Error('useAiWait는 AiWaitProvider 안에서만 사용할 수 있습니다.');
  }
  return context;
};
