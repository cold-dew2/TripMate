import { useTranslation } from 'react-i18next';
import type { TourReview } from '@/types/reviews';
import ReviewImageGrid from '@/shared/components/reviewImageGrid/ReviewImageGrid';
import './ReviewsList.css'

interface ReviewsProps {
  reviews?: TourReview[];
}
const ReviewsList = ({ reviews }: ReviewsProps) => {
  const { t } = useTranslation();

  if (!reviews?.length) {
    return <p className="review-empty">{t('review.empty')}</p>;
  }

  return (
    <ul className="review-list">
      {reviews.map((review, index) => (
        <li key={index}>
          <div className="review-item">
            <div className="review-header">
              <div className="review-title">
                <p>{review.userNm}</p>
                <div className="review-score">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <img
                      key={star}
                      src={
                        star <= review.reviewScore
                          ? "/icons/icon_star.png"
                          : "/icons/icon_star_gray.png"
                      }
                      alt=""
                    />
                  ))}
                </div>
              </div>
              <div className="review-date">{review.creatDt}</div>
            </div>
          </div>
          <div className="review-content">{review.reviewContent}</div>
          {review.imgUrls && <ReviewImageGrid urls={review.imgUrls.split(',')} />}
        </li>
      ))}
    </ul>
  )
}

export default ReviewsList