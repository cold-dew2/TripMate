import { useParams } from 'react-router-dom'
import usePlaceDetail from '../../hooks/usePlaceDetail';
import { useTranslation } from 'react-i18next';
import Tab from '@/shared/components/tab/Tab';
import Introduce from './components/introduce/Introduce';
import Information from './components/information/Information';
import Header from './components/header/Header';
import useReview from '../../hooks/useReveiws';
import Reviews from './components/reviews/Reviews';


const PlaceDetail = () => {
  const { t } = useTranslation();
  const { tourId } = useParams<{ tourId: string }>();
  const { data: place, isLoading, isError } = usePlaceDetail(tourId ?? "");
  const {
    data: reviews,
    isLoading: reviewIsLoading,
    isError: reviewIsError,
  } = useReview(tourId ?? "");

  if (isLoading) return <div>{tourId}로딩</div>;
  if (isError || !place) return <div>{tourId}nodata</div>;

  const tabs = [
    { id: "tab1", label: t("place.introduce") },
    { id: "tab2", label: t("place.info") },
    { id: "tab3", label: t("place.review") },
  ];

  return (
    <div className="place-detail">
      <Header place={place} />

      <div className="detail-content">
        <Tab tabs={tabs} type="scroll" />

        <div className="tab-contents">
          <div className="tab-content" id="tab1" tabIndex={-1}>
            <Introduce />
          </div>
          <div className="tab-content" id="tab2" tabIndex={-1}>
            <Information place={place} />
          </div>
          <div className="tab-content" id="tab3" tabIndex={-1}>
            {reviewIsLoading && <p>리뷰 불러오는 중...</p>}

            {reviewIsError && <p>리뷰를 불러오지 못했습니다.</p>}

            {!reviewIsLoading && !reviewIsError && reviews && (
              <Reviews reviews={reviews} />
            )}
          </div>

        </div>
      </div>
    </div>
  );
};

export default PlaceDetail; 