import { Link, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next';
import useMoimDetail from '../../hooks/useMoimDetail'
import useApplyMoim from '../../hooks/useApplyMoim';
import useMoimReviews from '../../hooks/useMoimReviews';
import Header from './components/header/Header';
import DaySchedule from '@/shared/components/daySchedule/DaySchedule';
import Button from '@/shared/components/button/Button';
import { resolveImageUrl } from '@/shared/utils/url';
import './MoimDetail.css';

const MoimDetail = () => {
  const { t } = useTranslation();
  const { moimId } = useParams<{ moimId: string }>();
  const { data: result, isLoading, isError } = useMoimDetail(moimId!);
  const applyMoim = useApplyMoim(moimId!);
  const { data: reviews } = useMoimReviews(moimId!);

  if (isLoading) return <p className="moim-detail-status">{t('account.loading')}</p>;
  if (isError || !result) return <p className="moim-detail-status">{t('common.loadError')}</p>;

  const moim = result.data;
  const cate = result.cate ?? [];
  const plan = result.plan ?? [];
  const joinStatus = result.joinStatus;
  const hasApplied = !!joinStatus;
  const isApproved = joinStatus?.stateCd === 'Y';

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
      window.alert(t('moim.shareCopied'));
    } catch {
      window.alert(url);
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
        <h2>{t('moim.scheduleTitle')}</h2>
        {days.length === 0 ? (
          <p className="moim-detail-empty">{t('moim.step3.emptyView')}</p>
        ) : (
          days.map((date, index) => (
            <DaySchedule
              key={date}
              day={index + 1}
              date={date}
              items={planByDay[date].map((item) => ({ id: `${date}-${item.tourNm}`, time: item.rmks, placeName: t(item.tourNm) }))}
            />
          ))
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
                {review.imgUrls && (
                  <ul className="moim-detail-review-images">
                    {review.imgUrls.split(',').map((url, index) => (
                      <li key={url}><img src={resolveImageUrl(url)} alt={t('image.reviewPhoto', { index: index + 1 })} /></li>
                    ))}
                  </ul>
                )}
              </li>
            ))}
          </ul>
        </section>
      )}

      <div className="moim-detail-actions">
        {hasApplied ? (
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
            onClick={() => applyMoim.mutate()}
            disabled={applyMoim.isPending}
          />
        )}
      </div>
    </div>
  );
};

export default MoimDetail;
