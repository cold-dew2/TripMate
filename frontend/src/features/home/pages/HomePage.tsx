import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import Card from '@/shared/components/card/Card';
import ContentTitle from '@/shared/components/contentTitle/ContentTitle';
import "./HomePage.css";
import SpotCard from '@/shared/components/spotCard/SpotCard';
import usePlace from '@/features/home/hooks/usePlace';
import MoimCard from '@/shared/components/moimCard/MoimCard';
import useMoim from '@/features/home/hooks/useMoim';

const categories = [
  { id: "culture", icon: "🏛️", title: "category.culture" },
  { id: "nature", icon: "⛺", title: "category.nature" },
  { id: "food", icon: "🍜", title: "category.food" },
  { id: "beach", icon: "🌊", title: "category.beach" },
  { id: "night", icon: "🌙", title: "category.night" },
];


const HomePage = () => {
  const { t } = useTranslation();
  const { data: spots = [], isLoading: isPlaceLoading, isError: isPlaceError } = usePlace();
  const { data: moims = [], isLoading: isMoimLoading,
    isError: isMoimError, } = useMoim();

  return (
    <>
      <section className="mt-20">
        <Card className="icon-card">
          {categories.map((category) => (
            // 링크는 임시값
            <Link to={`/meetings?category=${category.id}`} key={category.id} className="icon-item">
              <span className="icon">{category.icon}</span>
                <p className="title">{t(category.title)}</p>
            </Link>
          ))}
        </Card>
      </section>

      <section>
        <ContentTitle title="인기 여행지" href="/place?filter=popular" linkText="전체"/>

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
                  <SpotCard imageUrl={`/images/places/${spot.tourId}.jpeg`} title={t(spot.tourNm)} place={`${t(spot.roadAddr)}`} rating={spot.avgScore} badge={t(spot.cateNm)} />
                </Link>
              </li>
            ))
          )}
        </ul>
      </section>

      <section>
        <ContentTitle title="인기 소모임" href="/meeting?filter=popular" linkText="전체" />

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
                  <MoimCard badge={t(moim.cateNm)} imageUrl={`/images/places/${moim.moimId}.jpeg`} title={t(moim.moimTitle)} date={t(moim.moimStartDt)} member={t(moim.memberCnt)} views={moim.visitCnt}/>
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