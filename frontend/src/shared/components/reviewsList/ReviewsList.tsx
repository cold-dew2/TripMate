import { useTranslation } from 'react-i18next';
import type { TourReview } from '@/types/reviews';
import './ReviewsList.css'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

interface ReviewsProps {
  reviews?: TourReview[];
}
const ReviewsList = ({ reviews }: ReviewsProps) => {
  const { t } = useTranslation();
  return (
    <ul className="review-list">
      {reviews?.map((review, index) => (
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
          {review.imgUrls && (
            <ul className="review-images">
              {review.imgUrls.split(',').map((url, index) => (
                <li key={url}><img src={`${API_BASE_URL}${url}`} alt={t('image.reviewPhoto', { index: index + 1 })} /></li>
              ))}
            </ul>
          )}
        </li>
      ))}
    </ul>
  )
}

export default ReviewsList