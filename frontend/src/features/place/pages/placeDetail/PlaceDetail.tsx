import { useParams } from 'react-router-dom'
import usePlaceDetail from '../../hooks/usePlaceDetail';
import { useTranslation } from 'react-i18next';
import './PlaceDetail.css'

const PlaceDetail = () => {
  const { t } = useTranslation();
  const { tourId } = useParams<{ tourId: string }>();
  const { data: place, isLoading, isError } = usePlaceDetail(tourId ?? "");

  if (isLoading) return <div>{tourId}로딩</div>;
  if (isError || !place) return <div>{tourId}nodata</div>;

  return (
    <div className="detail-content">
      <div className="detail-header">
        <div className="detail-img">
          {/* <img src={`place.firstImage`} alt={`${place.tourNm}의 이미지`} /> */}
          <img src={`/images/places/${place.tourId}.jpeg`} alt="" />
        </div>
        <div className="detail-info">
          <div className="place-name">{t(place.tourNm)}</div>
          <div className="place-addr">{t(place.roadAddr)}</div>
          <div className="place-avgScore">
            <img src="/icons/icon_star.png" alt="" />
            <span>{place.avgScore}</span>
          </div>
        </div>
      </div>
      {tourId}
    </div>
  );
};

export default PlaceDetail; 