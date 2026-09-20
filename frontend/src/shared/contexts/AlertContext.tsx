import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import AlertModal from '@/shared/components/alertModal/AlertModal';
import { onBackendUnreachable } from '@/shared/api/networkStatus';

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
  const { t } = useTranslation();
  const [state, setState] = useState<AlertState | null>(null);

  const showAlert = useCallback((message: string, options?: AlertOptions) => {
    setState({ message, title: options?.title });
  }, []);

  // 백엔드 연결이 아예 안 될 때(서버 다운, 네트워크 단절) 화면마다 제각각인 에러 메시지
  // 대신 앱 전체에 동일한 점검 안내를 띄운다. 여러 요청이 동시에 실패해도 이미 다른
  // 팝업이 떠 있으면 덮어쓰지 않도록 함수형 업데이트로 한 번만 표시한다.
  useEffect(() => {
    return onBackendUnreachable(() => {
      setState((prev) => prev ?? { message: t('common.serviceDown') });
    });
  }, [t]);

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

