import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import AlertModal from '@/shared/components/alertModal/AlertModal';

interface AlertOptions {
  title?: string;
}

interface AlertContextValue {
  showAlert: (message: string, options?: AlertOptions) => void;
}

const AlertContext = createContext<AlertContextValue | null>(null);

// window.alert()는 브라우저 기본 팝업이라 앱 디자인과 안 맞고 눈에 잘 안 띄어서,
// 앱 전체에서 공유하는 중앙정렬 팝업으로 대체하기 위한 컨텍스트.
export const AlertProvider = ({ children }: { children: ReactNode }) => {
  const [state, setState] = useState<{ message: string; title?: string } | null>(null);

  const showAlert = useCallback((message: string, options?: AlertOptions) => {
    setState({ message, title: options?.title });
  }, []);

  const value = useMemo(() => ({ showAlert }), [showAlert]);

  return (
    <AlertContext.Provider value={value}>
      {children}
      {state && (
        <AlertModal
          message={state.message}
          title={state.title}
          onClose={() => setState(null)}
        />
      )}
    </AlertContext.Provider>
  );
};

export const useAlert = () => {
  const context = useContext(AlertContext);
  if (!context) {
    throw new Error('useAlert는 AlertProvider 안에서만 사용할 수 있습니다.');
  }
  return context;
};
