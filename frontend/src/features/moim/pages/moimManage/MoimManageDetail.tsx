import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import FilterTabs from '@/shared/components/filterTabs/FilterTabs';
import ChatRoom from '@/features/chat/components/ChatRoom';
import useMoimDetail from '../../hooks/useMoimDetail';
import useMoimMembers from '../../hooks/useMoimMembers';
import useUpdateMoimMember from '../../hooks/useUpdateMoimMember';
import DaySchedule from '@/shared/components/daySchedule/DaySchedule';
import './MoimManageDetail.css';

type ManageTab = 'applicants' | 'chat' | 'schedule';

const MoimManageDetail = () => {
  const { t } = useTranslation();
  const { moimId } = useParams<{ moimId: string }>();
  const [tab, setTab] = useState<ManageTab>('applicants');
  const [selected, setSelected] = useState<Set<string>>(new Set());

  const { data: result } = useMoimDetail(moimId!);
  const { data: members, isLoading, isError } = useMoimMembers(moimId!);
  const [actionError, setActionError] = useState('');
  const updateMember = useUpdateMoimMember(moimId!);

  const pendingMembers = (members ?? []).filter((m) => m.roleCd !== 'A' && m.stateCd !== 'Y');
  const allSelected = pendingMembers.length > 0 && selected.size === pendingMembers.length;

  const toggleSelectAll = () => {
    setSelected(allSelected ? new Set() : new Set(pendingMembers.map((m) => m.userId)));
  };

  const toggleSelect = (userId: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(userId)) next.delete(userId);
      else next.add(userId);
      return next;
    });
  };

  const approveSelected = async () => {
    setActionError('');
    try {
      await Promise.all(Array.from(selected).map((userId) => updateMember.mutateAsync({ userId, approve: true })));
      setSelected(new Set());
    } catch {
      setActionError(t('moim.approveFailed'));
    }
  };
  const plan = result?.plan ?? [];
  const planByDay = plan.reduce<Record<string, typeof plan>>((acc, item) => {
    (acc[item.startDt] ??= []).push(item);
    return acc;
  }, {});
  const days = Object.keys(planByDay).sort();

  return (
    <div className="manage-detail">
      {result && <h2 className="manage-detail-title">{result.data.moimTitle}</h2>}

      <FilterTabs
        options={[
          { id: 'applicants', label: t('moim.applicantList') },
          { id: 'chat', label: t('moim.moimChat') },
          { id: 'schedule', label: t('moim.scheduleEdit') },
        ]}
        activeId={tab}
        onChange={(id) => setTab(id as ManageTab)}
      />

      {tab === 'applicants' && (
        <section className="manage-detail-section">
          {isLoading ? (
            <p className="manage-loading">{t('account.loading')}</p>
          ) : isError ? (
            <p className="manage-loading">{t('common.loadError')}</p>
          ) : pendingMembers.length === 0 ? (
            <p className="manage-loading">{t('moim.noApplicants')}</p>
          ) : (
            <>
              {actionError && <p className="manage-action-error" role="alert">{actionError}</p>}
              <div className="applicant-list-head">
                <span>{t('moim.applicantList')} · {t('moim.waitingCount', { count: pendingMembers.length })}</span>
                <button type="button" onClick={toggleSelectAll}>
                  {allSelected ? t('common.deselectAll') : t('common.selectAll')}
                </button>
              </div>
              {selected.size > 0 && (
                <button
                  type="button"
                  className="applicant-approve-selected"
                  disabled={updateMember.isPending}
                  onClick={approveSelected}
                >
                  {t('moim.approveSelected', { count: selected.size })}
                </button>
              )}
              <ul className="applicant-list">
                {pendingMembers.map((member) => (
                  <li key={member.userId}>
                    <label className="applicant-checkbox">
                      <input
                        type="checkbox"
                        checked={selected.has(member.userId)}
                        onChange={() => toggleSelect(member.userId)}
                        aria-label={t('moim.selectApplicant', { name: member.userNm })}
                      />
                    </label>
                    <div className="applicant-avatar" aria-hidden="true">🙂</div>
                    <span className="applicant-name">{member.userNm}</span>
                    <div className="applicant-actions">
                      <button
                        type="button"
                        className="applicant-approve"
                        aria-label={t('moim.approve')}
                        disabled={updateMember.isPending}
                        onClick={() => {
                          setActionError('');
                          updateMember.mutate({ userId: member.userId, approve: true }, {
                            onError: () => setActionError(t('moim.approveFailed')),
                          });
                        }}
                      >
                        ✓
                      </button>
                      <button
                        type="button"
                        className="applicant-reject"
                        aria-label={t('moim.reject')}
                        disabled={updateMember.isPending}
                        onClick={() => {
                          setActionError('');
                          updateMember.mutate({ userId: member.userId, approve: false }, {
                            onError: () => setActionError(t('moim.approveFailed')),
                          });
                        }}
                      >
                        ✕
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>
      )}

      {tab === 'chat' && (
        <section className="manage-detail-chat-wrap">
          <ChatRoom roomId={`moim-${moimId}`} title={result ? result.data.moimTitle : undefined} />
        </section>
      )}

      {tab === 'schedule' && (
        <section className="manage-detail-section">
          {days.length === 0 ? (
            <p className="manage-loading">{t('moim.step3.emptyView')}</p>
          ) : (
            days.map((date, index) => (
              <DaySchedule
                key={date}
                day={index + 1}
                date={date}
                items={planByDay[date].map((item) => ({ id: `${date}-${item.tourNm}`, time: item.rmks, placeName: item.tourNm }))}
              />
            ))
          )}
          <p className="manage-coming-soon">{t('moim.scheduleEditComingSoon')}</p>
        </section>
      )}
    </div>
  );
};

export default MoimManageDetail;
