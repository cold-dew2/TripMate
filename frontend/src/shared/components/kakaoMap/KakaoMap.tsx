import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { loadKakaoMap } from '@/shared/utils/kakaoMap';
import './KakaoMap.css';

interface KakaoMapProps {
  latitude?: number;
  longitude?: number;
  address: string;
  title: string;
  level?: number;
}

const KakaoMap = ({ latitude, longitude, address, title, level = 4 }: KakaoMapProps) => {
  const { t } = useTranslation();
  const containerRef = useRef<HTMLDivElement>(null);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    let cancelled = false;

    const renderMap = (kakaoSdk: typeof kakao, lat: number, lng: number) => {
      if (cancelled || !containerRef.current) return;
      const center = new kakaoSdk.maps.LatLng(lat, lng);
      const map = new kakaoSdk.maps.Map(containerRef.current, { center, level });
      new kakaoSdk.maps.Marker({ position: center, map });
      setHasError(false);
    };

    loadKakaoMap()
      .then((kakaoSdk) => {
        if (cancelled) return;

        if (latitude != null && longitude != null) {
          renderMap(kakaoSdk, latitude, longitude);
          return;
        }

        const geocoder = new kakaoSdk.maps.services.Geocoder();
        geocoder.addressSearch(address, (result, status) => {
          if (cancelled) return;
          if (status === "OK" && result[0]) {
            renderMap(kakaoSdk, Number(result[0].y), Number(result[0].x));
          } else {
            setHasError(true);
          }
        });
      })
      .catch(() => {
        if (!cancelled) setHasError(true);
      });

    return () => {
      cancelled = true;
    };
  }, [latitude, longitude, address, level]);

  return (
    <div className="kakao-map-wrapper">
      <div
        ref={containerRef}
        className="kakao-map"
        role="img"
        aria-label={t('image.alt', { title })}
      />
      {hasError && <div className="kakao-map-fallback">{t('place.mapUnavailable')}</div>}
    </div>
  );
};

export default KakaoMap;
