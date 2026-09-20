import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import ContentTitle from '@/shared/components/contentTitle/ContentTitle';
import RegionBanner from '@/shared/components/regionBanner/RegionBanner';
import "./HomePage.css";
import SpotCard from '@/shared/components/spotCard/SpotCard';
import usePlace from '@/features/home/hooks/usePlace';
import MoimCard from '@/shared/components/moimCard/MoimCard';
import useMoim from '@/features/home/hooks/useMoim';
import useUser from '@/shared/hooks/useUser';
import useMyTodaySchedule from '@/features/moim/hooks/useMyTodaySchedule';
import TodayScheduleSheet from '@/features/home/components/todayScheduleSheet/TodayScheduleSheet';
import { resolveImageUrl } from '@/shared/utils/url';
import { translateCategoryList } from '@/shared/utils/category';
import Card from '@/shared/components/card/Card';

const HomePage = () => {
  const { t } = useTranslation();
  const { data: spots = [], isLoading: isPlaceLoading, isError: isPlaceError } = usePlace();
  const { data: moims = [], isLoading: isMoimLoading,
    isError: isMoimError, } = useMoim();

  const { data: user } = useUser();
  // 동시에 여러 모임이 오늘 진행 중일 수 있어 배열로 받는다.
  const { data: todaySchedule = [] } = useMyTodaySchedule(!!user);
  const [isTodaySheetOpen, setIsTodaySheetOpen] = useState(false);

  const bannerLabel = todaySchedule.length === 1
    ? todaySchedule[0].moimTitle
    : t('home.todayScheduleCount', { count: todaySchedule.length });

  return (
    <>
      {todaySchedule.length > 0 && (
        <button type="button" className="home-today-banner" onClick={() => setIsTodaySheetOpen(true)}>
          <span className="home-today-banner-badge">{t('home.todayScheduleBadge')}</span>
          <span className="home-today-banner-title">{bannerLabel}</span>
          <span className="home-today-banner-cta">{t('home.todayScheduleViewBtn')}</span>
        </button>
      )}

      {isTodaySheetOpen && todaySchedule.length > 0 && (
        <TodayScheduleSheet
          moims={todaySchedule}
          onClose={() => setIsTodaySheetOpen(false)}
        />
      )}

      <section className="home-banner-section">
        <RegionBanner
          eyebrow={t("home.sejongEyebrow")}
          title={t("home.sejongTitle")}
          desc={t("home.sejongDesc")}
          href="/moimList/sejong"
        />
      </section>

      <section>
        <ContentTitle title={t("home.popularPlaces")} href="/placeList" linkText={t("home.viewAll")} />

        <ul className="spot-list">
          {isPlaceLoading ? (
            Array.from({ length: 4 }).map((_, index) => (
              <li key={index}>
                <SpotCard loading />
              </li>
            ))
          ) : isPlaceError ? (
            <li>
              <Card error={t("home.errorMsg")}/>
            </li>
          ) : (
            spots.slice(0, 4)?.map(spot => (
              <li key={spot.tourId}>
                <Link to={`/place/${spot.tourId}`}>
                  <SpotCard imageUrl={spot.firstImage} title={spot.tourNm} place={spot.roadAddr} rating={spot.avgScore} badge={translateCategoryList(spot.cateNm, t)} />
                </Link>
              </li>
            ))
          )}
        </ul>
      </section>

      <section>
        <ContentTitle title={t("home.popularMoims")} href="/moimList" linkText={t("home.viewAll")} />

        <ul className="moim-list">
          {isMoimLoading ? (
            Array.from({ length: 4 }).map((_, index) => (
              <li key={index}>
                <MoimCard loading />
              </li>
            ))
          ) : isMoimError ? (
            <li>
              <Card error={t("home.errorMsg")}/>
            </li>
          ) : (
            moims.slice(0, 4)?.map(moim => (
              <li key={moim.moimId}>
                <Link to={`/moim/${moim.moimId}`}>
                  <MoimCard badge={translateCategoryList(moim.cateNm, t)} imageUrl={resolveImageUrl(moim.imageUrl)} title={moim.moimTitle} date={moim.moimStartDt} member={moim.memberCnt} views={moim.visitCnt}/>
                </Link>
              </li>
            ))
          )}
        </ul>

      </section>
    </>
  )
}

export default HomePage