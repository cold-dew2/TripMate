import type { PlaceDetail } from '@/types/place';
import { useTranslation } from 'react-i18next';
import './Header.css'

interface InformationProps { 
  place: PlaceDetail; 
}


const Header = ({ place }: InformationProps) => {
  const { t } = useTranslation();

  return (
    <div className="detail-header">
      <div className="detail-img">
        <img
          src={place.firstImage || "/images/places/no-image.png"}
          alt={t('image.alt', { title: place.tourNm })}
          onError={(event) => { event.currentTarget.src = "/images/places/no-image.png"; }}
        />
      </div>
      <div className="detail-info">
        <div className="place-name">{place.tourNm}</div>
        <div className="place-addr">{place.roadAddr}</div>
        <div className="place-avgScore">
          <img src="/icons/icon_star.png" alt="" />
          <span>{place.avgScore}</span>
        </div>
      </div>
    </div>
  )
}

export default Header