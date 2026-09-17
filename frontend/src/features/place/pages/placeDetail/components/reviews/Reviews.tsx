import ReviewsList from '@/shared/components/reviewsList/ReviewsList';
import type { TourReview } from '@/types/reviews';
import { useTranslation } from 'react-i18next';
import { useState } from 'react';
import ReviewBottomSheet from '../reviewBottomSheet/ReviewBottomSheet';

interface ReviewsProps {
  reviews: TourReview[];
  tourId: string;
}

const Reviews = ({ reviews, tourId }: ReviewsProps) => {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  return (
    <>
      <div className="title-wrap">
        <p>{t("place.review")}</p>
        <span><button type="button" onClick={() => setIsOpen(true)}>{t("place.reviewsBtn")}</button></span>
      </div>

      <div className="reviews-content">
        <div className="reviews">

        </div>

        <ReviewsList reviews={reviews} />
      </div>
      {isOpen && <ReviewBottomSheet tourId={tourId} onClose={() => setIsOpen(false)} />}
    </>
  )
}

export default Reviews
