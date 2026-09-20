import { useTranslation } from 'react-i18next';
import './PageState.css';

interface PageStateProps {
  status: 'loading' | 'error' | 'empty';
  message?: string;
  onRetry?: () => void;
  // 화면 전체를 대체하는 상태면 true(기본값)로 화면 중앙에 오도록 세로로도 정렬하고,
  // 탭/섹션 안에 끼워 넣는 작은 상태면 false로 넘겨 과도하게 커지지 않게 한다.
  fullScreen?: boolean;
}

// 화면/섹션 단위 로딩·에러·빈 상태를 앱 전체에서 동일한 톤으로 보여주기 위한 공통 컴포넌트.
// 데이터를 기다리는 동안 빈 화면만 떠 있으면 사용자가 "에러난 줄" 오해하기 쉬우므로,
// 로딩 중임을 항상 스피너+문구로 명확히 알린다.
const DEFAULT_KEY = {
  loading: 'common.loading',
  error: 'common.loadError',
  empty: 'common.empty',
} as const;

const PageState = ({ status, message, onRetry, fullScreen = true }: PageStateProps) => {
  const { t } = useTranslation();
  const text = message ?? t(DEFAULT_KEY[status]);

  return (
    <div
      className={`page-state page-state-${status} ${fullScreen ? 'page-state-fullscreen' : ''}`}
      role={status === 'error' ? 'alert' : 'status'}
    >
      {status === 'loading' && <span className="page-state-spinner" aria-hidden="true" />}
      <p>{text}</p>
      {status === 'error' && onRetry && (
        <button type="button" className="page-state-retry" onClick={onRetry}>
          {t('common.retry')}
        </button>
      )}
    </div>
  );
};

export default PageState;
