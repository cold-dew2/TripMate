import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { loadKakaoMap } from '@/shared/utils/kakaoMap';
import './ItineraryMap.css';

export interface ItineraryStop {
  id: string;
  placeName: string;
  roadAddr?: string;
}

interface ItineraryMapProps {
  stops: ItineraryStop[];
}

// Step5(일정 확인)에 넣는 지도. PlaceDetail의 KakaoMap과 달리 방문 순서대로
// 여러 관광지를 한 지도에 번호 마커로 같이 보여줘야 해서 별도 컴포넌트로 뒀다.
const ItineraryMap = ({ stops }: ItineraryMapProps) => {
  const { t } = useTranslation();
  const containerRef = useRef<HTMLDivElement>(null);
  const [hasError, setHasError] = useState(false);

  const geocodableStops = stops.filter((stop) => stop.roadAddr);
  const noStops = geocodableStops.length === 0;

  useEffect(() => {
    let cancelled = false;
    if (noStops) return;

    loadKakaoMap()
      .then((kakaoSdk) => {
        if (cancelled || !containerRef.current) return;

        const map = new kakaoSdk.maps.Map(containerRef.current, {
          center: new kakaoSdk.maps.LatLng(37.5665, 126.978),
          level: 8,
        });
        const geocoder = new kakaoSdk.maps.services.Geocoder();
        const bounds = new kakaoSdk.maps.LatLngBounds();
        let settled = 0;

        geocodableStops.forEach((stop, index) => {
          geocoder.addressSearch(stop.roadAddr!, (result, status) => {
            settled += 1;
            if (!cancelled && status === 'OK' && result[0]) {
              const position = new kakaoSdk.maps.LatLng(Number(result[0].y), Number(result[0].x));
              new kakaoSdk.maps.Marker({ position, map });
              new kakaoSdk.maps.CustomOverlay({
                position,
                map,
                yAnchor: 2.6,
                content: `<span class="itinerary-map-badge">${index + 1}</span>`,
              });
              bounds.extend(position);
            }

            if (!cancelled && settled === geocodableStops.length) {
              if (bounds.isEmpty()) {
                setHasError(true);
              } else {
                map.setBounds(bounds);
              }
            }
          });
        });
      })
      .catch(() => {
        if (!cancelled) setHasError(true);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [noStops, geocodableStops.map((s) => `${s.id}:${s.roadAddr}`).join('|')]);

  return (
    <div className="itinerary-map-wrap">
      <div ref={containerRef} className="itinerary-map" />
      {(hasError || noStops) && <div className="itinerary-map-fallback">{t('moimCreate.step5.mapUnavailable')}</div>}
    </div>
  );
};

export default ItineraryMap;
