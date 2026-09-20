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

const AlertModal = ({ message, title, isConfirm, confirmText, cancelText, onConfirm, onClose }: AlertModalProps) => {
  const { t } = useTranslation();

  return (
    <div className="alert-modal-dim" role="presentation" onMouseDown={onClose}>
      <div
        className="alert-modal"
        role="alertdialog"
        aria-modal="true"
        aria-label={title ?? message}
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
