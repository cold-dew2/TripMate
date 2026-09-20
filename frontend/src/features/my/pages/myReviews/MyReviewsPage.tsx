import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import useMyReviewList, { type MyReviewSort } from '../../hooks/useMyReviewList';
import FilterTabs from '@/shared/components/filterTabs/FilterTabs';
import PageState from '@/shared/components/pageState/PageState';
import ReviewImageGrid from '@/shared/components/reviewImageGrid/ReviewImageGrid';
import './MyReviewsPage.css';

const MyReviewsPage = () => {
  const { t } = useTranslation();
  const [sort, setSort] = useState<MyReviewSort>('latest');
  const { data: reviews, isLoading, isError } = useMyReviewList(sort);

  return (
    <div className="my-reviews-page">
      <FilterTabs
        options={[
          { id: 'latest', label: t('my.sortLatest') },
          { id: 'rating', label: t('my.sortRating') },
        ]}
        activeId={sort}
        onChange={(id) => setSort(id as MyReviewSort)}
      />
      {isLoading ? (
        <PageState status="loading" fullScreen={false} />
      ) : isError ? (
        <PageState status="error" fullScreen={false} />
      ) : !reviews?.length ? (
        <PageState status="empty" message={t('my.noReceivedReviews')} fullScreen={false} />
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
                {review.imgUrls && <ReviewImageGrid urls={review.imgUrls.split(',')} />}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default MyReviewsPage;
