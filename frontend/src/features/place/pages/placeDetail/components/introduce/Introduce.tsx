import type { PlaceDetail } from '@/types/place';
import { useTranslation } from 'react-i18next';
import KakaoMap from '@/shared/components/kakaoMap/KakaoMap';

const Introduce = ({ place }: { place: PlaceDetail }) => {
  const { t } = useTranslation();
  return (
    <div className="place-introduce">
      <p className="place-overview">{place.overview || t('place.overviewFallback', { name: place.tourNm })}</p>
      <div className="title-wrap"><p>{t('place.location')}</p></div>
      <div className="place-map">
        <KakaoMap
          latitude={place.latitude}
          longitude={place.longitude}
          address={place.roadAddr}
          title={place.tourNm}
        />
      </div>
      <p className="map-address">{place.roadAddr} {place.detailAddr}</p>
    </div>
  )
}

export default Introduce
