import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import AlertModal from '@/shared/components/alertModal/AlertModal';

interface AlertOptions {
  title?: string;
}

interface ConfirmOptions {
  title?: string;
  confirmText?: string;
  cancelText?: string;
  onConfirm?: () => void;
  onCancel?: () => void;
}

interface AlertContextValue {
  showAlert: (message: string, options?: AlertOptions) => void;
  showConfirm: (message: string, options?: ConfirmOptions) => void;
}

interface AlertState {
  message: string;
  title?: string;
  confirmText?: string;
  cancelText?: string;
  onConfirm?: () => void;
  onCancel?: () => void;
  isConfirm?: boolean;
}

const AlertContext = createContext<AlertContextValue | null>(null);

// window.alert()/confirm()은 브라우저 기본 팝업이라 앱 디자인과 안 맞고 눈에 잘 안 띄어서,
// 앱 전체에서 공유하는 중앙정렬 팝업으로 대체하기 위한 컨텍스트.
export const AlertProvider = ({ children }: { children: ReactNode }) => {
  const [state, setState] = useState<AlertState | null>(null);

  const showAlert = useCallback((message: string, options?: AlertOptions) => {
    setState({ message, title: options?.title });
  }, []);

  const showConfirm = useCallback((message: string, options?: ConfirmOptions) => {
    setState({
      message,
      title: options?.title,
      confirmText: options?.confirmText,
      cancelText: options?.cancelText,
      onConfirm: options?.onConfirm,
      onCancel: options?.onCancel,
      isConfirm: true,
    });
  }, []);

  const value = useMemo(() => ({ showAlert, showConfirm }), [showAlert, showConfirm]);

  return (
    <AlertContext.Provider value={value}>
      {children}
      {state && (
        <AlertModal
          message={state.message}
          title={state.title}
          isConfirm={state.isConfirm}
          confirmText={state.confirmText}
          cancelText={state.cancelText}
          onConfirm={() => {
            state.onConfirm?.();
            setState(null);
          }}
          onClose={() => {
            if (state.isConfirm) state.onCancel?.();
            setState(null);
          }}
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

