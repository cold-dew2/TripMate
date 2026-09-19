import { useParams } from 'react-router-dom'
import usePlaceDetail from '../../hooks/usePlaceDetail';
import { useTranslation } from 'react-i18next';
import Tab from '@/shared/components/tab/Tab';
import Introduce from './components/introduce/Introduce';
import Information from './components/information/Information';
import Header from './components/header/Header';
import useReview from '../../hooks/useReveiws';
import Reviews from './components/reviews/Reviews';
import PageState from '@/shared/components/pageState/PageState';
import './PlaceDetail.css';


const PlaceDetail = () => {
  const { t } = useTranslation();
  const { tourId } = useParams<{ tourId: string }>();
  const { data: place, isLoading, isError, refetch } = usePlaceDetail(tourId ?? "");
  // 관광지 상세(이름/주소/개요 등)만으로 화면을 그리고, 이용 정보(운영시간 등)는
  // Information에서 각자 별도로 불러온다(느린 AI 호출이 전체 화면을 막지 않도록).
  const {
    data: reviewPages,
    isLoading: reviewIsLoading,
    isError: reviewIsError,
  } = useReview(tourId ?? "");
  const reviews = reviewPages?.pages.flat();

  if (isLoading) return <PageState status="loading" message={t('place.detailLoading')} />;
  if (isError || !place) return <PageState status="error" message={t('place.detailError')} onRetry={() => refetch()} />;

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
            <Introduce place={place} />
          </div>
          <div className="tab-content" id="tab2" tabIndex={-1}>
            <Information tourId={tourId ?? ""} />
          </div>
          <div className="tab-content" id="tab3" tabIndex={-1}>
            {reviewIsLoading && <PageState status="loading" fullScreen={false} />}

            {reviewIsError && <PageState status="error" fullScreen={false} />}

            {!reviewIsLoading && !reviewIsError && reviews && (
              <Reviews reviews={reviews} tourId={tourId ?? ""} />
            )}
          </div>

        </div>
      </div>
    </div>
  );
};

export default PlaceDetail; 
