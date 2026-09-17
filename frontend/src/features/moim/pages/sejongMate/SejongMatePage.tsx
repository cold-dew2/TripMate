import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import AsyncList from "@/shared/components/asyncList/AsyncList";
import FilterTabs, { type FilterOption } from "@/shared/components/filterTabs/FilterTabs";
import MoimCard from "@/shared/components/moimCard/MoimCard";
import Button from "@/shared/components/button/Button";
import useMoimList from "../../hooks/useMoimList";
import type { MoimCreatePrefill } from "@/types/moim";
import "./SejongMatePage.css";

const STYLE_FILTERS: FilterOption[] = [
  { id: "all", label: "전체" },
  { id: "NAT", label: "🌿 자연" },
  { id: "NIG", label: "🌙 야경" },
  { id: "ACT", label: "🚲 자전거" },
  { id: "PHO", label: "📷 사진" },
  { id: "RES", label: "🍴 맛집" },
  { id: "CUL", label: "🏛️ 도시탐방" },
];

interface CourseStop {
  name: string;
  address: string;
}

interface Course {
  eyebrow: string;
  title: string;
  duration: string;
  themeId: string;
  dscr: string;
  stops: CourseStop[];
}

const COURSES: Record<string, Course> = {
  all: {
    eyebrow: "세종 추천 코스",
    title: "세종 초록 산책 코스",
    duration: "약 5시간",
    themeId: "theme1",
    dscr: "세종호수공원 → 국립세종수목원 → 금강보행교를 함께 걷는 코스예요. 편한 신발 신고 오세요!",
    stops: [
      { name: "세종호수공원", address: "세종특별자치시 한누리대로 2500" },
      { name: "국립세종수목원", address: "세종특별자치시 수목원길 136" },
      { name: "금강보행교", address: "세종특별자치시 나성동" },
    ],
  },
  NAT: {
    eyebrow: "자연 추천 코스",
    title: "세종 초록 산책 코스",
    duration: "약 5시간",
    themeId: "theme1",
    dscr: "세종호수공원 → 국립세종수목원 → 금강보행교를 함께 걷는 코스예요. 편한 신발 신고 오세요!",
    stops: [
      { name: "세종호수공원", address: "세종특별자치시 한누리대로 2500" },
      { name: "국립세종수목원", address: "세종특별자치시 수목원길 136" },
      { name: "금강보행교", address: "세종특별자치시 나성동" },
    ],
  },
  NIG: {
    eyebrow: "야경 추천 코스",
    title: "세종 야경 산책 코스",
    duration: "약 2시간",
    themeId: "theme6",
    dscr: "호수공원부터 금강보행교까지, 사진 찍으면서 천천히 걸어요.",
    stops: [
      { name: "세종호수공원", address: "세종특별자치시 한누리대로 2500" },
      { name: "금강보행교", address: "세종특별자치시 나성동" },
    ],
  },
  ACT: {
    eyebrow: "자전거 추천 코스",
    title: "세종 어울링 라이딩 코스",
    duration: "약 3시간 20분",
    themeId: "theme4",
    dscr: "공공자전거 어울링을 타고 세종시 대표 명소를 둘러보는 코스예요.",
    stops: [
      { name: "세종호수공원", address: "세종특별자치시 한누리대로 2500" },
      { name: "세종중앙공원", address: "세종특별자치시 한누리대로 300" },
      { name: "국립세종수목원", address: "세종특별자치시 수목원길 136" },
      { name: "금강보행교", address: "세종특별자치시 나성동" },
    ],
  },
  PHO: {
    eyebrow: "사진 추천 코스",
    title: "세종 포토스팟 코스",
    duration: "약 3시간",
    themeId: "theme6",
    dscr: "금강보행교와 국립세종수목원, 세종을 대표하는 포토스팟을 함께 담아봐요.",
    stops: [
      { name: "금강보행교", address: "세종특별자치시 나성동" },
      { name: "국립세종수목원", address: "세종특별자치시 수목원길 136" },
    ],
  },
  RES: {
    eyebrow: "맛집 추천 코스",
    title: "세종 나성동 맛집 코스",
    duration: "약 2시간 30분",
    themeId: "theme3",
    dscr: "나성동 맛집 거리를 둘러보고 호수공원까지 산책하는 코스예요.",
    stops: [
      { name: "나성동 맛집거리", address: "세종특별자치시 나성동" },
      { name: "세종호수공원", address: "세종특별자치시 한누리대로 2500" },
    ],
  },
  CUL: {
    eyebrow: "도시탐방 추천 코스",
    title: "세종 행정도시 투어",
    duration: "약 4시간",
    themeId: "theme2",
    dscr: "대통령기록관부터 국립세종도서관까지, 행정중심도시 세종을 둘러보는 코스예요.",
    stops: [
      { name: "대통령기록관", address: "세종특별자치시 다솜1로 95" },
      { name: "정부세종청사", address: "세종특별자치시 한누리대로 2130" },
      { name: "국립세종도서관", address: "세종특별자치시 다솜로 261" },
      { name: "세종호수공원", address: "세종특별자치시 한누리대로 2500" },
    ],
  },
};

const SejongMatePage = () => {
  const { t } = useTranslation();
  const [activeStyle, setActiveStyle] = useState("all");

  const { data, isLoading, isError } = useMoimList("all", "세종");
  const moims = useMemo(() => data?.pages.flat() ?? [], [data]);

  const filtered = useMemo(() => {
    if (activeStyle === "all") return moims;
    return moims.filter((moim) => moim.cateCd?.split(",").includes(activeStyle));
  }, [moims, activeStyle]);

  const course = COURSES[activeStyle] ?? COURSES.all;
  const coursePrefill: MoimCreatePrefill = {
    title: course.title,
    dscr: course.dscr,
    themeId: course.themeId,
    courseStops: course.stops,
  };

  return (
    <div className="sejong-mate-page">
      <p className="sejong-mate-eyebrow">📍 {t("sejongMate.eyebrow")}</p>

      <FilterTabs
        options={STYLE_FILTERS}
        activeId={activeStyle}
        onChange={(id) => setActiveStyle(id as string)}
      />

      <section className="sejong-course-card">
        <p className="sejong-course-eyebrow">🌿 {course.eyebrow}</p>
        <h2>{course.title}</h2>
        <p className="sejong-course-route">
          {course.stops.map((stop, index) => (
            <span key={stop.name}>
              {index > 0 && <span aria-hidden="true"> → </span>}
              {stop.name}
            </span>
          ))}
        </p>
        <p className="sejong-course-duration">🚶 {course.duration}</p>
        <Button as={Link} to="/createMoim" state={coursePrefill} text={t("sejongMate.courseCta")} />
      </section>

      <h2 className="sejong-mate-section-title">🔥 {t("sejongMate.recruitingTitle")}</h2>
      <ul className="moim-list">
        <AsyncList
          isLoading={isLoading}
          isError={isError}
          data={filtered}
          renderSkeleton={() => <MoimCard loading />}
          renderItem={(moim) => (
            <li key={moim.moimId}>
              <Link to={`/moim/${moim.moimId}`}>
                <MoimCard
                  badge={moim.cateNm}
                  imageUrl={moim.imageUrl || "/images/places/no-image.png"}
                  title={moim.moimTitle}
                  desc={moim.moimDscr}
                  date={moim.moimStartDt}
                  place={moim.region}
                  member={moim.memberCnt}
                  maxMember={String(moim.maxMember ?? '')}
                  userNm={moim.userNm}
                  userRating={String(moim.avgScore ?? '')}
                />
              </Link>
            </li>
          )}
          errorMsg={t("moim.errorMsg")}
          emptyMsg={t("sejongMate.emptyMsg")}
        />
      </ul>
    </div>
  );
};

export default SejongMatePage;
