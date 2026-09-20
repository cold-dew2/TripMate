import { useMemo } from 'react';
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
import Button from '@/shared/components/button/Button';
import PageState from '@/shared/components/pageState/PageState';
import ReviewImageGrid from '@/shared/components/reviewImageGrid/ReviewImageGrid';
import { useAlert } from '@/shared/contexts/AlertContext';
import './MoimDetail.css';

const MoimDetail = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { moimId } = useParams<{ moimId: string }>();
  const { data: result, isLoading, isError, refetch } = useMoimDetail(moimId!);
  const applyMoim = useApplyMoim(moimId!);
  const { data: reviews } = useMoimReviews(moimId!);
  const { data: user } = useUser();
  const { showAlert, showConfirm } = useAlert();
  const transportRecommend = useMoimTransportRecommend(moimId!);
  const legsByKey = useMemo(() => {
    const map = new Map<string, TransportLeg>();
    (transportRecommend.data ?? []).forEach((leg) => {
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
        <div className="moim-detail-host-avatar" aria-hidden="true" />
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
        {transportRecommend.isSuccess && transportRecommend.data.length === 0 && (
          <p className="moim-detail-transport-empty">{t('moim.transportAnalysisEmpty')}</p>
        )}
        {days.length === 0 ? (
          <p className="moim-detail-empty">{t('moim.step3.emptyView')}</p>
        ) : (
          days.map((date, index) => {
            const day = index + 1;
            return (
              <DaySchedule
                key={date}
                day={day}
                date={date}
                items={planByDay[date].map((item) => ({ id: `${date}-${item.tourNm}`, time: item.rmks, placeName: item.tourNm, tourId: item.tourId }))}
                renderBetween={(prevItem, item) => {
                  const leg = legsByKey.get(`${day}-${prevItem.tourId}-${item.tourId}`);
                  return leg ? <TransportLegView leg={leg} /> : null;
                }}
              />
            );
          })
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
