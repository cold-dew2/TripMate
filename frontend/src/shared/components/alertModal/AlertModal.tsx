import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import Button from '@/shared/components/button/Button';
import './AlertModal.css';

interface AlertModalProps {
  message: string;
  title?: string;
  isConfirm?: boolean;
  confirmText?: string;
  cancelText?: string;
  onConfirm?: () => void;
  onClose: () => void;
}

const FOCUSABLE_SELECTOR = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';

const AlertModal = ({ message, title, isConfirm, confirmText, cancelText, onConfirm, onClose }: AlertModalProps) => {
  const { t } = useTranslation();
  const dialogRef = useRef<HTMLDivElement>(null);

  // 이 모달은 앱 전체에서 알림/확인 팝업으로 쓰이는데, 키보드/스크린리더 사용자를
  // 위한 처리가 전혀 없었다: 열려도 포커스가 그대로 배경에 남아있고, Esc로 닫을 수
  // 없고, Tab이 모달 밖 배경 요소로 빠져나갈 수 있었다. 열릴 때 모달 안으로 포커스를
  // 옮기고, 닫히면 원래 포커스였던 요소로 되돌리며, Tab이 모달 안에서만 순환하도록
  // 최소한의 포커스 트랩을 둔다.
  useEffect(() => {
    const previouslyFocused = document.activeElement as HTMLElement | null;
    const dialog = dialogRef.current;
    // 확인/취소가 함께 있는 다이얼로그는 취소(첫 번째 버튼)에 기본 포커스를 둬서,
    // 삭제/탈퇴 같은 확인 다이얼로그에서 Enter를 잘못 눌러 바로 실행되는 걸 막는다.
    const focusables = dialog?.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR);
    (focusables?.[0] ?? dialog)?.focus();

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
        return;
      }
      if (event.key !== 'Tab' || !dialog) return;
      const items = Array.from(dialog.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR));
      if (items.length === 0) return;
      const first = items[0];
      const last = items[items.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      previouslyFocused?.focus();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="alert-modal-dim" role="presentation" onMouseDown={onClose}>
      <div
        ref={dialogRef}
        className="alert-modal"
        role="alertdialog"
        aria-modal="true"
        aria-label={title ?? message}
        tabIndex={-1}
        onMouseDown={(event) => event.stopPropagation()}
      >
        {title && <p className="alert-modal-title">{title}</p>}
        <p className="alert-modal-message">{message}</p>
        {isConfirm ? (
          <div className="alert-modal-actions">
            <Button text={cancelText ?? t('common.cancel')} variant="secondary" onClick={onClose} />
            <Button text={confirmText ?? t('common.confirm')} onClick={onConfirm} />
          </div>
        ) : (
          <Button text={t('common.confirm')} onClick={onClose} />
        )}
      </div>
    </div>
  );
};

export default AlertModal;
