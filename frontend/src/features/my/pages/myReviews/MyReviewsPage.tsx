import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import useMyReviewList, { type MyReviewSort } from '../../hooks/useMyReviewList';
import FilterTabs from '@/shared/components/filterTabs/FilterTabs';
import PageState from '@/shared/components/pageState/PageState';
import ReviewImageGrid from '@/shared/components/reviewImageGrid/ReviewImageGrid';
import { resolveImageUrl } from '@/shared/utils/url';
import './MyReviewsPage.css';

const MyReviewsPage = () => {
  const { t } = useTranslation();
  const [sort, setSort] = useState<MyReviewSort>('latest');
  const { data: reviews, isLoading, isError } = useMyReviewList(sort);
  // 업로드 기록은 남아있지만 실제 파일이 없어져 깨진 이미지 아이콘으로 뜨는 경우
  // 조용히 기본 아바타로 대체한다.
  const [brokenAvatars, setBrokenAvatars] = useState<Set<number>>(new Set());

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
              {review.profileImgUrl && !brokenAvatars.has(index) ? (
                <img
                  className="my-review-avatar"
                  src={resolveImageUrl(review.profileImgUrl)}
                  alt=""
                  onError={() => setBrokenAvatars((prev) => new Set(prev).add(index))}
                />
              ) : (
                <div className="my-review-avatar" aria-hidden="true" />
              )}
              <div className="my-review-body">
                <div className="my-review-top">
                  <strong>{review.userNm}</strong>
                  <span className="stars" aria-hidden="true">{'★'.repeat(review.reviewScore)}</span>
                  <time>{review.createDt}</time>
                </div>
                <div className="mypage-review-content">
                  <p>{review.reviewContent}</p>
                  {review.imgUrls && <ReviewImageGrid urls={review.imgUrls.split(',')} maxVisible={1} />} 
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default MyReviewsPage;
