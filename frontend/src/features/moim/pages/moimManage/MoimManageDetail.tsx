import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQueryClient } from '@tanstack/react-query';
import FilterTabs from '@/shared/components/filterTabs/FilterTabs';
import ChatRoom from '@/features/chat/components/ChatRoom';
import useMoimDetail from '../../hooks/useMoimDetail';
import useMoimMembers from '../../hooks/useMoimMembers';
import useUser from '@/shared/hooks/useUser';
import useUpdateMoimMember from '../../hooks/useUpdateMoimMember';
import { useTransportRecommend, type TransportLeg } from '../../hooks/useTransportRecommend';
import DaySchedule, { type ScheduleItem } from '@/shared/components/daySchedule/DaySchedule';
import TransportLegView from '@/shared/components/transportLeg/TransportLegView';
import Button from '@/shared/components/button/Button';
import PageState from '@/shared/components/pageState/PageState';
import Skeleton from '@/shared/components/skeleton/Skeleton';
import { useAlert } from '@/shared/contexts/AlertContext';
import { apiClient } from '@/shared/api/client';
import { addDays, formatMonthDay } from '@/shared/utils/date';
import { resolveImageUrl } from '@/shared/utils/url';
import { getApiLang } from '@/shared/utils/lang';
import type { Place } from '@/types/place';
import './MoimManageDetail.css';

// 일정이 바뀔 때마다(추가/삭제/시간 변경) 날짜는 이미 day 단위로 묶여 있으니, 하루 안에서는
// 시간(rmks) 순으로 다시 정렬해서 보여준다 — "일자+시간 순으로 조회".
const sortByTime = (items: ScheduleItem[]) => [...items].sort((a, b) => a.time.localeCompare(b.time));

const nextTime = (count: number) => {
  const hour = (10 + count * 3) % 24;
  return `${String(hour).padStart(2, '0')}:00`;
};

type ManageTab = 'applicants' | 'members' | 'chat' | 'schedule';
const MANAGE_TABS: ManageTab[] = ['applicants', 'members', 'chat', 'schedule'];

const MoimManageDetail = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { showAlert, showConfirm } = useAlert();
  const queryClient = useQueryClient();
  const { moimId } = useParams<{ moimId: string }>();
  const [isDeleting, setIsDeleting] = useState(false);
  const [searchParams, setSearchParams] = useSearchParams();
  const tabFromUrl = searchParams.get('tab') as ManageTab | null;
  const [tab, setTab] = useState<ManageTab>(
    tabFromUrl && MANAGE_TABS.includes(tabFromUrl) ? tabFromUrl : 'applicants'
  );

  const changeTab = (id: ManageTab) => {
    setTab(id);
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('tab', id);
      return next;
    });
  };
  const [selected, setSelected] = useState<Set<string>>(new Set());
  // 업로드 기록은 있는데 실제 파일이 없어져 깨진 이미지 아이콘으로 뜨는 경우를 대비한 안전장치.
  const [brokenAvatars, setBrokenAvatars] = useState<Set<string>>(new Set());

  const { data: result, isError: isDetailError, error: detailError } = useMoimDetail(moimId!);
  const { data: members, isLoading, isError } = useMoimMembers(moimId!);
  const { data: user } = useUser();
  const isHost = !!user && !!result && user.userId === result.data.userId;
  const [actionError, setActionError] = useState('');
  const updateMember = useUpdateMoimMember(moimId!);

  // 알림에 남아있는 링크 등으로 이미 삭제된 소모임의 관리 화면에 들어온 경우,
  // 삭제됐다는 걸 명확히 알리고 내 모임 관리 목록으로 돌려보낸다.
  useEffect(() => {
    if (isDetailError && (detailError as { code?: string } | null)?.code === 'MOIM_DELETED') {
      showAlert(t('moim.deletedNotice'));
      navigate('/moimManage', { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isDetailError, detailError]);

  const deleteMoim = () => {
    showConfirm(t('moim.deleteConfirm'), {
      confirmText: t('moim.delete'),
      onConfirm: async () => {
        setIsDeleting(true);
        const res = await apiClient.delete(`/moimList/${moimId}`);
        setIsDeleting(false);
        if (!res.success) {
          showAlert(t('moim.deleteFailed'));
          return;
        }
        // 삭제된 모임의 캐시가 남아있으면 목록/관리 화면으로 돌아갔을 때 잠깐이라도
        // 다시 보일 수 있어 함께 비운다.
        queryClient.invalidateQueries({ queryKey: ['myMoim'] });
        showAlert(t('moim.deleteSuccess'));
        navigate('/moimManage', { replace: true });
      },
    });
  };

  // 채팅과 마찬가지로, 알림을 거치지 않고 모임 관리 화면에 바로 들어와 신청자 목록을
  // 조회한 것만으로도 그 모임의 가입 신청 알림이 읽음 처리되도록 한다.
  useEffect(() => {
    if (!moimId) return;
    void apiClient.put(`/moimList/${moimId}/applicantsRead`, {}).then((res) => {
      if (res.success) {
        queryClient.invalidateQueries({ queryKey: ['unreadNotificationCount'] });
        queryClient.invalidateQueries({ queryKey: ['notifications'] });
      }
    });
  }, [moimId, queryClient]);

  const pendingMembers = (members ?? []).filter((m) => m.roleCd !== 'A' && m.stateCd !== 'Y');
  const approvedMembers = (members ?? []).filter((m) => m.stateCd === 'Y');
  const allSelected = pendingMembers.length > 0 && selected.size === pendingMembers.length;

  const kickMember = (member: { userId: string; userNm: string }) => {
    showConfirm(t('moim.kickConfirm', { name: member.userNm }), {
      confirmText: t('moim.kick'),
      onConfirm: () => {
        setActionError('');
        updateMember.mutate({ userId: member.userId, approve: false }, {
          onError: () => setActionError(t('moim.kickFailed')),
        });
      },
    });
  };

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
  const moimStartDt = result?.data.moimStartDt;
  const moimEndDt = result?.data.moimEndDt;

  const [itemsByDay, setItemsByDay] = useState<Record<number, ScheduleItem[]> | null>(null);
  const [addingDay, setAddingDay] = useState<number | null>(null);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchResults, setSearchResults] = useState<Place[]>([]);
  const [planSaving, setPlanSaving] = useState(false);
  const [planError, setPlanError] = useState('');

  // 여행 일자(시작일/종료일) 자체는 여기서 바꿀 수 없고, 모임 등록 시 정해진 기간에
  // 맞춰 며칠짜리 일정인지만 계산한다. 일정(장소/시간)만 추가·삭제·변경할 수 있다.
  const dayCount = moimStartDt && moimEndDt
    ? Math.max(1, Math.round((new Date(moimEndDt).getTime() - new Date(moimStartDt).getTime()) / 86400000) + 1)
    : 1;

  // 조회된 저장 일정을 한 번만 편집 가능한 로컬 상태로 옮겨온다(그 뒤로는 이 상태가
  // 편집의 기준이 되고, 저장 성공 시 서버 데이터를 다시 불러와 갱신한다).
  useEffect(() => {
    if (!result || itemsByDay !== null) return;

    const grouped: Record<number, ScheduleItem[]> = {};
    (result.plan ?? []).forEach((item) => {
      const dayIndex = moimStartDt
        ? Math.round((new Date(item.startDt).getTime() - new Date(moimStartDt).getTime()) / 86400000) + 1
        : 1;
      const list = grouped[dayIndex] ?? [];
      list.push({ id: `${item.tourId}-${item.startDt}-${item.rmks}`, time: item.rmks, placeName: item.tourNm, tourId: item.tourId, imageUrl: item.firstImage });
      grouped[dayIndex] = list;
    });

    Object.keys(grouped).forEach((day) => {
      grouped[Number(day)] = sortByTime(grouped[Number(day)]);
    });

    setItemsByDay(grouped);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [result]);

  const removeItem = (day: number, id: string) => {
    setItemsByDay((prev) => (prev ? { ...prev, [day]: (prev[day] ?? []).filter((item) => item.id !== id) } : prev));
  };

  const changeItemTime = (day: number, id: string, time: string) => {
    setItemsByDay((prev) => (prev
      ? { ...prev, [day]: sortByTime((prev[day] ?? []).map((item) => (item.id === id ? { ...item, time } : item))) }
      : prev));
  };

  const openSpotSearch = (day: number) => {
    setAddingDay(day);
    setSearchKeyword('');
    setSearchResults([]);
  };

  useEffect(() => {
    if (addingDay === null) return;
    const timer = window.setTimeout(async () => {
      const res = await apiClient.get<{ data: Place[] }>('/tourList/tourSearch', { page: 1, keyword: searchKeyword.trim(), lang: getApiLang() });
      if (res.success) setSearchResults(res.data.data ?? []);
    }, 300);
    return () => window.clearTimeout(timer);
  }, [addingDay, searchKeyword]);

  const addSpot = (place: Place) => {
    if (addingDay === null) return;
    setItemsByDay((prev) => {
      const base = prev ?? {};
      const dayItems = base[addingDay] ?? [];
      const newItem: ScheduleItem = {
        id: `${place.tourId}-${Date.now()}`,
        time: nextTime(dayItems.length),
        placeName: place.tourNm,
        imageUrl: place.firstImage,
        tourId: place.tourId,
      };
      return { ...base, [addingDay]: sortByTime([...dayItems, newItem]) };
    });
  };

  const savePlan = async () => {
    if (!itemsByDay || !moimStartDt || !moimId) return;
    setPlanSaving(true);
    setPlanError('');
    try {
      const items = Object.entries(itemsByDay)
        .filter(([day]) => Number(day) <= dayCount)
        .flatMap(([day, list]) =>
          list.map((item) => ({
            startDt: addDays(moimStartDt, Number(day) - 1),
            rmks: item.time,
            tourId: item.tourId ?? '',
          }))
        );
      const res = await apiClient.put(`/moimList/${moimId}/plan`, { items });
      if (!res.success) {
        setPlanError(t('moim.planSaveError'));
        return;
      }
      queryClient.invalidateQueries({ queryKey: ['moimDetail', moimId] });
      showAlert(t('moim.planSaved'));
    } finally {
      setPlanSaving(false);
    }
  };

  const totalItemCount = Object.values(itemsByDay ?? {}).reduce((sum, list) => sum + list.length, 0);

  const transportRecommend = useTransportRecommend();
  const legsByKey = useMemo(() => {
    const map = new Map<string, TransportLeg>();
    (transportRecommend.data ?? []).forEach((leg) => {
      map.set(`${leg.day}-${leg.fromTourId}-${leg.toTourId}`, leg);
    });
    return map;
  }, [transportRecommend.data]);

  const handleTransportRecommend = () => {
    if (!itemsByDay) return;
    const items = Object.entries(itemsByDay).flatMap(([day, list]) =>
      list.map((item) => ({
        day: Number(day),
        time: item.time,
        tourId: item.tourId ?? '',
        tourNm: item.placeName,
        roadAddr: '',
      }))
    );
    transportRecommend.mutate(items, {
      onError: (error) => {
        if ((error as { code?: string } | null)?.code === 'AI_UNAVAILABLE') {
          showAlert(t('common.aiUnavailable'));
        }
      },
    });
  };

  return (
    <div className="manage-detail">
      <h2 className="manage-detail-title">
        {result ? result.data.moimTitle : <Skeleton width="60%" height="20px" />}
      </h2>

      <FilterTabs
        options={[
          { id: 'applicants', label: t('moim.applicantList') },
          { id: 'members', label: t('moim.memberList') },
          { id: 'chat', label: t('moim.moimChat') },
          { id: 'schedule', label: t('moim.scheduleEdit') },
        ]}
        activeId={tab}
        onChange={(id) => changeTab(id as ManageTab)}
      />

      {tab === 'applicants' && (
        <section className="manage-detail-section">
          {isLoading ? (
            <PageState status="loading" fullScreen={false} />
          ) : isError ? (
            <PageState status="error" fullScreen={false} />
          ) : pendingMembers.length === 0 ? (
            <PageState status="empty" message={t('moim.noApplicants')} fullScreen={false} />
          ) : (
            <>
              {actionError && <p className="manage-action-error" role="alert">{actionError}</p>}
              <div className="applicant-list-head">
                <span>{t('moim.applicantList')} · {t('moim.waitingCount', { count: pendingMembers.length })}</span>
                {isHost && (
                  <button type="button" onClick={toggleSelectAll}>
                    {allSelected ? t('common.deselectAll') : t('common.selectAll')}
                  </button>
                )}
              </div>
              {isHost && selected.size > 0 && (
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
                    {isHost && (
                      <label className="applicant-checkbox">
                        <input
                          type="checkbox"
                          checked={selected.has(member.userId)}
                          onChange={() => toggleSelect(member.userId)}
                          aria-label={t('moim.selectApplicant', { name: member.userNm })}
                        />
                      </label>
                    )}
                    {member.profileImgUrl && !brokenAvatars.has(member.userId) ? (
                      <img
                        className="applicant-avatar"
                        src={resolveImageUrl(member.profileImgUrl)}
                        alt=""
                        onError={() => setBrokenAvatars((prev) => new Set(prev).add(member.userId))}
                      />
                    ) : (
                      <div className="applicant-avatar" aria-hidden="true">🙂</div>
                    )}
                    <span className="applicant-name">{member.userNm}</span>
                    {isHost && (
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
                    )}
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>
      )}

      {tab === 'members' && (
        <section className="manage-detail-section">
          {isLoading ? (
            <PageState status="loading" fullScreen={false} />
          ) : isError ? (
            <PageState status="error" fullScreen={false} />
          ) : approvedMembers.length === 0 ? (
            <PageState status="empty" message={t('moim.noMembers')} fullScreen={false} />
          ) : (
            <>
              {actionError && <p className="manage-action-error" role="alert">{actionError}</p>}
              <ul className="applicant-list">
                {approvedMembers.map((member) => (
                  <li key={member.userId}>
                    {member.profileImgUrl && !brokenAvatars.has(member.userId) ? (
                      <img
                        className="applicant-avatar"
                        src={resolveImageUrl(member.profileImgUrl)}
                        alt=""
                        onError={() => setBrokenAvatars((prev) => new Set(prev).add(member.userId))}
                      />
                    ) : (
                      <div className="applicant-avatar" aria-hidden="true">🙂</div>
                    )}
                    <span className="applicant-name">
                      {member.userNm}
                      {member.roleCd === 'A' && <span className="member-host-badge">{t('moim.hostBadge')}</span>}
                    </span>
                    {isHost && member.roleCd !== 'A' && (
                      <div className="applicant-actions">
                        <button
                          type="button"
                          className="applicant-kick"
                          aria-label={t('moim.kick')}
                          disabled={updateMember.isPending}
                          onClick={() => kickMember(member)}
                        >
                          {t('moim.kick')}
                        </button>
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>
      )}

      {tab === 'chat' && (
        <section className="manage-detail-chat-wrap">
          <ChatRoom roomId={`moim-${moimId}`} title={result ? result.data.moimTitle : undefined} showLeave={false} />
        </section>
      )}

      {tab === 'schedule' && (
        <section className="manage-detail-section">
          {!itemsByDay ? (
            <PageState status="loading" fullScreen={false} />
          ) : (
            <>
              <p className="schedule-date-range">{moimStartDt} ~ {moimEndDt}</p>
              {isHost && planError && <p className="manage-action-error" role="alert">{planError}</p>}

              {isHost && totalItemCount >= 2 && (
                <div className="manage-transport-trigger">
                  <Button
                    text={transportRecommend.isPending ? t('common.saving') : t('moim.transportRecommend')}
                    onClick={handleTransportRecommend}
                    disabled={transportRecommend.isPending}
                  />
                  {transportRecommend.isError && (
                    <p className="manage-action-error" role="alert">
                      {(transportRecommend.error as { code?: string } | null)?.code === 'AI_UNAVAILABLE'
                        ? t('common.aiUnavailable')
                        : t('moim.transportRecommendError')}
                    </p>
                  )}
                </div>
              )}

              {Array.from({ length: dayCount }, (_, index) => index + 1).map((day) => {
                const date = moimStartDt ? addDays(moimStartDt, day - 1) : '';
                return (
                  <div key={day}>
                    <DaySchedule
                      day={day}
                      date={date ? formatMonthDay(date) : `${day}`}
                      items={itemsByDay[day] ?? []}
                      mode={isHost ? "edit" : "view"}
                      onRemove={(id) => removeItem(day, id)}
                      onAddClick={() => openSpotSearch(day)}
                      onTimeChange={(id, time) => changeItemTime(day, id, time)}
                      renderBetween={(prev, item) => {
                        if (!prev.tourId || !item.tourId) return null;
                        const leg = legsByKey.get(`${day}-${prev.tourId}-${item.tourId}`);
                        return leg ? <TransportLegView leg={leg} /> : null;
                      }}
                    />
                    {isHost && addingDay === day && (
                      <div className="schedule-spot-search">
                        <input
                          type="text"
                          value={searchKeyword}
                          onChange={(event) => setSearchKeyword(event.target.value)}
                          placeholder={t('moimCreate.step4.searchPlaceholder')}
                          aria-label={t('moimCreate.step4.searchPlaceholder')}
                        />
                        <ul className="schedule-spot-results">
                          {searchResults.length === 0 ? (
                            <li className="schedule-spot-empty">{t('moimCreate.step4.empty')}</li>
                          ) : (
                            searchResults.map((place) => (
                              <li key={place.tourId}>
                                <span>{place.tourNm}</span>
                                <button type="button" onClick={() => addSpot(place)} aria-label={t('moimCreate.step3.addSchedule')}>+</button>
                              </li>
                            ))
                          )}
                        </ul>
                        <button type="button" className="schedule-spot-done" onClick={() => setAddingDay(null)}>
                          {t('common.confirm')}
                        </button>
                      </div>
                    )}
                  </div>
                );
              })}

              {isHost && (
                <div className="schedule-save-row">
                  <Button
                    text={planSaving ? t('common.saving') : t('common.submit')}
                    onClick={savePlan}
                    disabled={planSaving}
                  />
                </div>
              )}
            </>
          )}
        </section>
      )}

      {isHost && (
        <div className="manage-detail-delete-row">
          <button
            type="button"
            className="manage-delete-btn"
            onClick={deleteMoim}
            disabled={isDeleting}
          >
            {isDeleting ? t('common.saving') : t('moim.delete')}
          </button>
        </div>
      )}
    </div>
  );
};

export default MoimManageDetail;
