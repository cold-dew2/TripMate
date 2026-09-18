import { useTranslation } from 'react-i18next';
import useMyReviewList from '../../hooks/useMyReviewList';
import { resolveImageUrl } from '@/shared/utils/url';
import './MyReviewsPage.css';

const MyReviewsPage = () => {
  const { t } = useTranslation();
  const { data: reviews, isLoading, isError } = useMyReviewList();

  return (
    <div className="my-reviews-page">
      {isLoading ? (
        <p className="my-reviews-state">{t('account.loading')}</p>
      ) : isError ? (
        <p className="my-reviews-state">{t('common.loadError')}</p>
      ) : !reviews?.length ? (
        <p className="my-reviews-state">{t('common.loadError')}</p>
      ) : (
        <ul className="my-reviews-list">
          {reviews.map((review, index) => (
            <li key={index}>
              <div className="my-review-avatar" aria-hidden="true" />
              <div className="my-review-body">
                <div className="my-review-top">
                  <strong>{review.userNm}</strong>
                  <span className="stars" aria-hidden="true">{'★'.repeat(review.reviewScore)}</span>
                  <time>{review.createDt}</time>
                </div>
                <p>{review.reviewContent}</p>
                {review.imgUrls && (
                  <ul className="my-review-images">
                    {review.imgUrls.split(',').map((url, index) => (
                      <li key={url}><img src={resolveImageUrl(url)} alt={t('image.reviewPhoto', { index: index + 1 })} /></li>
                    ))}
                  </ul>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default MyReviewsPage;
