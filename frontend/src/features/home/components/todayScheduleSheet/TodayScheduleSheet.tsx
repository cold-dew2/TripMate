import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import Button from '@/shared/components/button/Button';
import type { MyTodayScheduleMoim } from '@/features/moim/hooks/useMyTodaySchedule';
import './TodayScheduleSheet.css';

interface TodayScheduleSheetProps {
  moims: MyTodayScheduleMoim[];
  onClose: () => void;
}

// 홈 화면에서 "오늘 진행 중인 내 소모임" 배너를 눌렀을 때 뜨는 바텀시트.
// 동시에 여러 모임이 진행 중일 수 있어 상단 탭으로 모임을 전환해서 보여준다.
const TodayScheduleSheet = ({ moims, onClose }: TodayScheduleSheetProps) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [activeIndex, setActiveIndex] = useState(0);
  const activeMoim = moims[activeIndex];

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  // 바텀시트가 열려 있는 동안 배경 스크롤을 막는다.
  useEffect(() => {
    const { overflow } = document.body.style;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = overflow;
    };
  }, []);

  if (!activeMoim) return null;

  return (
    <div className="bottom-sheet-dim" role="presentation" onMouseDown={onClose}>
      <section
        className="today-schedule-sheet"
        role="dialog"
        aria-modal="true"
        aria-label={t('home.todayScheduleTitle')}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="sheet-handle" />
        <button type="button" className="sheet-close-btn" onClick={onClose} aria-label={t('common.close')}>✕</button>

        <p className="today-schedule-eyebrow">{t('home.todayScheduleBadge')}</p>
        <h2>{t('home.todayScheduleTitle')}</h2>

        {moims.length > 1 && (
          <div className="today-schedule-tabs" role="tablist">
            {moims.map((moim, index) => (
              <button
                key={moim.moimId}
                type="button"
                role="tab"
                aria-selected={index === activeIndex}
                className={`today-schedule-tab${index === activeIndex ? ' active' : ''}`}
                onClick={() => setActiveIndex(index)}
              >
                {moim.moimTitle}
              </button>
            ))}
          </div>
        )}

        <div className="today-schedule-panel" key={activeMoim.moimId}>
          {moims.length === 1 && (
            <p className="today-schedule-moim-title">{activeMoim.moimTitle}</p>
          )}

          {activeMoim.items.length === 0 ? (
            <p className="today-schedule-empty">{t('home.todayScheduleEmpty')}</p>
          ) : (
            <ul className="today-schedule-timeline">
              {activeMoim.items.map((item, index) => (
                <li key={`${item.time}-${index}`} className="today-schedule-timeline-item">
                  <div className="today-schedule-timeline-marker">
                    <span className="today-schedule-dot" />
                    {index < activeMoim.items.length - 1 && <span className="today-schedule-line" />}
                  </div>
                  <div className="today-schedule-timeline-body">
                    <span className="today-schedule-time-chip">{item.time}</span>
                    <span className="today-schedule-place">{item.placeName}</span>
                  </div>
                </li>
              ))}
            </ul>
          )}

          <Button
            className="today-schedule-detail-btn"
            text={t('home.todayScheduleDetailBtn')}
            onClick={() => {
              onClose();
              navigate(`/moim/${activeMoim.moimId}`);
            }}
          />
        </div>
      </section>
    </div>
  );
};

export default TodayScheduleSheet;
