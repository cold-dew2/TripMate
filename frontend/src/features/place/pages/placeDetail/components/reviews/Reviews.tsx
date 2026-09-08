import ReviewsList from '@/shared/components/reviewsList/ReviewsList';
import type { TourReview } from '@/types/reviews';
import { useTranslation } from 'react-i18next';

interface ReviewsProps {
  reviews: TourReview[];
}

const Reviews = ({ reviews }: ReviewsProps) => {
  const { t } = useTranslation();
  return (
    <>
      <div className="tab-title">
        <p>{t("place.review")}</p>
        <span><button type="button">{t("place.reviewsBtn")}</button></span>
      </div>

      <div className="reviews-content">
        <div className="reviews">

        </div>

        <ReviewsList reviews={reviews} />
      </div>
    </>
  )
}

export default Reviews