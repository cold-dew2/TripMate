import type { PlaceDetail } from '@/types/place';
import { useTranslation } from 'react-i18next';

const Introduce = ({ place }: { place: PlaceDetail }) => {
  const { t } = useTranslation();
  const mapUrl = place.latitude && place.longitude
    ? `https://www.openstreetmap.org/export/embed.html?bbox=${place.longitude - 0.01}%2C${place.latitude - 0.005}%2C${place.longitude + 0.01}%2C${place.latitude + 0.005}&layer=mapnik&marker=${place.latitude}%2C${place.longitude}`
    : `https://www.openstreetmap.org/export/embed.html?search=${encodeURIComponent(place.roadAddr)}`;
  return (
    <div className="place-introduce">
      <p className="place-overview">{place.overview ? t(place.overview) : t('place.overviewFallback', { name: place.tourNm })}</p>
      <div className="title-wrap"><p>{t('place.location')}</p></div>
      <div className="place-map"><iframe title={`${place.tourNm} map`} src={mapUrl} loading="lazy" /></div>
      <p className="map-address">{place.roadAddr} {place.detailAddr}</p>
    </div>
  )
}

export default Introduce
