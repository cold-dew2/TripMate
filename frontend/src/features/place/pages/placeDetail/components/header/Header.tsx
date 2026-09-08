import type { PlaceDetail } from '@/types/place';
import { useTranslation } from 'react-i18next';

interface InformationProps { 
  place: PlaceDetail; 
}


const Header = ({ place }: InformationProps) => {
  const { t } = useTranslation();

  return (
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
  )
}

export default Header