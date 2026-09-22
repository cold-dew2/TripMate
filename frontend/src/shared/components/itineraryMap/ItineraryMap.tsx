import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { loadKakaoMap } from '@/shared/utils/kakaoMap';
import './ItineraryMap.css';

export interface ItineraryStop {
  id: string;
  placeName: string;
  roadAddr?: string;
  // 며칠짜리 일정을 한 지도에 같이 표시할 때(예: 소모임 상세) 날짜별로 색과 번호를
  // 구분하기 위한 값. 하루짜리 일정(Step5의 일자별 지도 등)에서는 안 넘겨도 된다.
  day?: number;
}

interface ItineraryMapProps {
  stops: ItineraryStop[];
}

// 날짜별로 배지 색을 다르게 써서(6가지를 넘는 날짜는 순환) 위에 나열된 "Day 1/Day 2..."
// 일정 목록과 지도의 번호가 헷갈리지 않게 한다. 이 6개는 모두 흰 글자 대비 4.5:1
// 이상을 확인해 배지 안 숫자가 잘 보이도록 골랐다.
const DAY_COLORS = ['#4b3fe4', '#2f6fed', '#c93a3f', '#0a7a70', '#8f5c08', '#d6336c'];
const colorForDay = (day: number) => DAY_COLORS[(day - 1) % DAY_COLORS.length];

// Step5(일정 확인)/소모임 상세에 넣는 지도. PlaceDetail의 KakaoMap과 달리 방문 순서대로
// 여러 관광지를 한 지도에 번호 마커로 같이 보여줘야 해서 별도 컴포넌트로 뒀다.
const ItineraryMap = ({ stops }: ItineraryMapProps) => {
  const { t } = useTranslation();
  const containerRef = useRef<HTMLDivElement>(null);
  const [hasError, setHasError] = useState(false);

  const geocodableStops = stops.filter((stop) => stop.roadAddr);
  const noStops = geocodableStops.length === 0;
  // 배지 색만으로는 어떤 색이 몇 일차인지 알 수 없어서(예: 보라색이 1일차인지
  // 2일차인지 지도만 봐서는 구분이 안 된다는 피드백), 실제로 날짜가 둘 이상 섞여
  // 있을 때만 범례를 보여준다. 날짜 구분이 없는 호출부(day 미지정)나 하루짜리
  // 일정에서는 범례가 필요 없다.
  const distinctDays = Array.from(new Set(
    geocodableStops.map((stop) => stop.day).filter((day): day is number => day != null)
  )).sort((a, b) => a - b);
  // 날짜가 바뀔 때마다 배지 번호가 1부터 다시 시작하도록, 같은 day 안에서의 순번을
  // 미리 계산해둔다(day가 없는 호출부는 전체를 하루짜리로 보고 이어서 매긴다). id로
  // 찾기보다 배열 인덱스를 그대로 맞춰두면, 같은 날 같은 관광지를 두 번 넣어 id가
  // 겹치는 경우에도 번호가 안 꼬인다.
  const dayCounts = new Map<string, number>();
  const orderInDayByIndex = geocodableStops.map((stop) => {
    const key = String(stop.day ?? 'default');
    const next = (dayCounts.get(key) ?? 0) + 1;
    dayCounts.set(key, next);
    return next;
  });

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
              const color = colorForDay(stop.day ?? 1);
              new kakaoSdk.maps.Marker({ position, map });
              new kakaoSdk.maps.CustomOverlay({
                position,
                map,
                yAnchor: 2.6,
                content: `<span class="itinerary-map-badge" style="background:${color}">${orderInDayByIndex[index]}</span>`,
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
  }, [noStops, geocodableStops.map((s) => `${s.id}:${s.roadAddr}:${s.day ?? ''}`).join('|')]);

  return (
    <div className="itinerary-map-wrap">
      <div ref={containerRef} className="itinerary-map" />
      {(hasError || noStops) && <div className="itinerary-map-fallback">{t('moimCreate.step5.mapUnavailable')}</div>}
      {distinctDays.length > 1 && (
        <ul className="itinerary-map-legend">
          {distinctDays.map((day) => (
            <li key={day}>
              <span className="itinerary-map-legend-dot" style={{ background: colorForDay(day) }} aria-hidden="true" />
              {t('moimCreate.step4.dayLabel', { day })}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default ItineraryMap;
