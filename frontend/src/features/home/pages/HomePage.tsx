import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import ContentTitle from '@/shared/components/contentTitle/ContentTitle';
import RegionBanner from '@/shared/components/regionBanner/RegionBanner';
import "./HomePage.css";
import SpotCard from '@/shared/components/spotCard/SpotCard';
import usePlace from '@/features/home/hooks/usePlace';
import MoimCard from '@/shared/components/moimCard/MoimCard';
import useMoim from '@/features/home/hooks/useMoim';

const HomePage = () => {
  const { t } = useTranslation();
  const { data: spots = [], isLoading: isPlaceLoading, isError: isPlaceError } = usePlace();
  const { data: moims = [], isLoading: isMoimLoading,
    isError: isMoimError, } = useMoim();

  return (
    <>
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
            <li className="spot-error">{t("home.errorMsg")}</li>
          ) : (
            spots.slice(0, 4)?.map(spot => (
              <li key={spot.tourId}>
                <Link to={`/place/${spot.tourId}`}>
                  <SpotCard imageUrl={spot.firstImage} title={t(spot.tourNm)} place={`${t(spot.roadAddr)}`} rating={spot.avgScore} badge={t(spot.cateNm)} />
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
            <li className="spot-error">{t("home.errorMsg")}</li>
          ) : (
            moims.slice(0, 4)?.map(moim => (
              <li key={moim.moimId}>
                <Link to={`/moim/${moim.moimId}`}>
                  <MoimCard badge={t(moim.cateNm)} imageUrl={`/images/places/M001.jpeg`} title={moim.moimTitle} date={moim.moimStartDt} member={moim.memberCnt} views={moim.visitCnt}/>
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