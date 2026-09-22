import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next';
import useMoimDetail from '../../hooks/useMoimDetail'
import useApplyMoim from '../../hooks/useApplyMoim';
import useMoimReviews from '../../hooks/useMoimReviews';
import useMoimTransportRecommend from '../../hooks/useMoimTransportRecommend';
import type { TransportLeg } from '../../hooks/useTransportRecommend';
import useUser from '@/shared/hooks/useUser';
import Header from './components/header/Header';
import DaySchedule from '@/shared/components/daySchedule/DaySchedule';
import TransportLegView from '@/shared/components/transportLeg/TransportLegView';
import ItineraryMap from '@/shared/components/itineraryMap/ItineraryMap';
import Button from '@/shared/components/button/Button';
import PageState from '@/shared/components/pageState/PageState';
import ReviewImageGrid from '@/shared/components/reviewImageGrid/ReviewImageGrid';
import { useAlert } from '@/shared/contexts/AlertContext';
import { resolveImageUrl } from '@/shared/utils/url';
import useTranslationCatchup from '@/shared/hooks/useTranslationCatchup';
import useAiWaitNotice from '@/shared/hooks/useAiWaitNotice';
import './MoimDetail.css';

const MoimDetail = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { moimId } = useParams<{ moimId: string }>();
  const { data: result, isLoading, isError, error, refetch } = useMoimDetail(moimId!);
  useTranslationCatchup(refetch, !isLoading);
  const applyMoim = useApplyMoim(moimId!);
  const { data: reviews } = useMoimReviews(moimId!);
  const { data: user } = useUser();
  const { showAlert, showConfirm } = useAlert();

  // 알림에 남아있는 링크 등으로 이미 삭제된 소모임에 들어온 경우, 빈 화면이나
  // 일반 에러 화면 대신 삭제됐다는 걸 명확히 알리고 내 모임 관리 목록으로 돌려보낸다.
  useEffect(() => {
    if (isError && (error as { code?: string } | null)?.code === 'MOIM_DELETED') {
      showAlert(t('moim.deletedNotice'));
      navigate('/moimManage', { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isError, error]);
  // 업로드 기록은 있는데 실제 파일이 없어져 깨진 이미지 아이콘으로 뜨는 경우를 대비한 안전장치.
  const [hostAvatarBroken, setHostAvatarBroken] = useState(false);
  const transportRecommend = useMoimTransportRecommend(moimId!);
  useAiWaitNotice(transportRecommend.isFetching, t('moim.transportAnalysisWait'));
  const legsByKey = useMemo(() => {
    const map = new Map<string, TransportLeg>();
    (transportRecommend.data?.legs ?? []).forEach((leg) => {
      map.set(`${leg.day}-${leg.fromTourId}-${leg.toTourId}`, leg);
    });
    return map;
  }, [transportRecommend.data]);

  if (isLoading) return <PageState status="loading" />;
  if (isError || !result) return <PageState status="error" onRetry={() => refetch()} />;

  const moim = result.data;
  const cate = result.cate ?? [];
  const plan = result.plan ?? [];
  const joinStatus = result.joinStatus;
  const hasApplied = !!joinStatus;
  const isApproved = joinStatus?.stateCd === 'Y';
  const isHost = !!user && user.userId === moim.userId;

  const planByDay = plan.reduce<Record<string, typeof plan>>((acc, item) => {
    (acc[item.startDt] ??= []).push(item);
    return acc;
  }, {});
  const days = Object.keys(planByDay).sort();

  const handleShare = async () => {
    const url = `${window.location.origin}/moim/${moimId}`;
    if (navigator.share) {
      try {
        await navigator.share({ title: moim.moimTitle, url });
        return;
      } catch {
        // 공유 취소 시 링크 복사로 폴백
      }
    }
    try {
      await navigator.clipboard.writeText(url);
      showAlert(t('moim.shareCopied'));
    } catch {
      showAlert(url);
    }
  };

  return (
    <div className="moim-detail">
      <Header moim={moim} cate={cate} onShare={handleShare} />

      <Link to={`/users/${moim.userId}`} className="moim-detail-host">
        {moim.hostProfileImgUrl && !hostAvatarBroken ? (
          <img
            className="moim-detail-host-avatar"
            src={resolveImageUrl(moim.hostProfileImgUrl)}
            alt=""
            onError={() => setHostAvatarBroken(true)}
          />
        ) : (
          <div className="moim-detail-host-avatar" aria-hidden="true" />
        )}
        <span>{moim.userNm}</span>
        {moim.reviewScore != null && (
          <span className="moim-detail-host-score">★ {Number(moim.reviewScore).toFixed(1)}</span>
        )}
      </Link>

      <section className="moim-detail-plan">
        <div className="moim-detail-plan-header">
          <h2>{t('moim.scheduleTitle')}</h2>
          {isApproved && plan.length >= 2 && (
            <Button
              size="sm"
              variant="secondary"
              text={transportRecommend.isFetching ? t('common.saving') : t('moim.transportAnalysisBtn')}
              onClick={async () => {
                const res = await transportRecommend.refetch();
                if (res.error) {
                  const code = (res.error as { code?: string } | null)?.code;
                  showAlert(code === 'AI_UNAVAILABLE' ? t('common.aiUnavailable') : t('moim.transportAnalysisError'));
                }
              }}
              disabled={transportRecommend.isFetching}
            />
          )}
        </div>
        {transportRecommend.isSuccess && transportRecommend.data.legs.length === 0 && (
          <p className="moim-detail-transport-empty">
            {transportRecommend.data.code === 'AI_TEMPORARILY_UNAVAILABLE'
              ? t('moim.transportAnalysisUnavailable')
              : t('moim.transportAnalysisEmpty')}
          </p>
        )}
        {days.length === 0 ? (
          <p className="moim-detail-empty">{t('moim.step3.emptyView')}</p>
        ) : (
          <>
            {days.map((date, index) => {
              const day = index + 1;
              return (
                <DaySchedule
                  key={date}
                  day={day}
                  date={date}
                  items={planByDay[date].map((item) => ({ id: `${date}-${item.tourNm}`, time: item.rmks, placeName: item.tourNm, tourId: item.tourId, imageUrl: item.firstImage }))}
                  renderBetween={(prevItem, item) => {
                    const leg = legsByKey.get(`${day}-${prevItem.tourId}-${item.tourId}`);
                    return leg ? <TransportLegView leg={leg} /> : null;
                  }}
                />
              );
            })}
            <div className="moim-detail-map">
              <ItineraryMap
                stops={days.flatMap((date, index) =>
                  planByDay[date].map((item) => ({
                    id: `${date}-${item.tourNm}`,
                    placeName: item.tourNm,
                    roadAddr: item.roadAddr,
                    day: index + 1,
                  }))
                )}
              />
            </div>
          </>
        )}
      </section>

      {!!reviews?.length && (
        <section className="moim-detail-reviews">
          <h2>{t('place.review')}</h2>
          <ul className="moim-detail-review-list">
            {reviews.map((review, index) => (
              <li key={index}>
                <div className="moim-detail-review-top">
                  <strong>{review.userNm}</strong>
                  <span className="stars" aria-hidden="true">{'★'.repeat(review.reviewScore)}</span>
                  <time>{review.createDt}</time>
                </div>
                <p>{review.reviewContent}</p>
                {review.imgUrls && <ReviewImageGrid urls={review.imgUrls.split(',')} />}
              </li>
            ))}
          </ul>
        </section>
      )}

      <div className="moim-detail-actions">
        {isHost ? (
          <Button as={Link} to={`/moimManage/${moimId}`} text={t('moim.manageBtn')} />
        ) : hasApplied ? (
          <>
            <Button text={isApproved ? t('moim.applied') : t('moim.applying')} variant="secondary" disabled />
            {isApproved ? (
              <Button as={Link} to={`/chat/moim-${moimId}`} state={{ title: moim.moimTitle }} text={t('moim.chatWithMembers')} />
            ) : (
              <Button text={t('moim.chatWithMembers')} disabled />
            )}
          </>
        ) : (
          <Button
            text={applyMoim.isPending ? t('common.saving') : t('moim.applyBtn')}
            onClick={() => applyMoim.mutate(undefined, {
              onError: (error) => {
                const code = (error as { code?: string })?.code;
                if (code === 'NEED_LOGIN') {
                  showConfirm(t('moim.applyNeedLogin'), {
                    confirmText: t('account.login'),
                    onConfirm: () => navigate('/auth'),
                  });
                } else {
                  showAlert(t('moim.applyError'));
                }
              },
            })}
            disabled={applyMoim.isPending}
          />
        )}
      </div>
    </div>
  );
};

export default MoimDetail;
