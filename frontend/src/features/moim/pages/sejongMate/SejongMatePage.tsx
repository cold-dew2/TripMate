import { useMemo } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import AsyncList from "@/shared/components/asyncList/AsyncList";
import FilterTabs, { type FilterOption } from "@/shared/components/filterTabs/FilterTabs";
import MoimCard from "@/shared/components/moimCard/MoimCard";
import Button from "@/shared/components/button/Button";
import useMoimList from "../../hooks/useMoimList";
import type { MoimCreatePrefill } from "@/types/moim";
import { translateCategoryList } from "@/shared/utils/category";
import useUrlState from "@/shared/hooks/useUrlState";
import { useIsLoggedIn } from "@/shared/hooks/useIsLoggedIn";
import "./SejongMatePage.css";

const STYLE_FILTER_IDS: { id: string; icon: string; label: string }[] = [
  { id: "all", icon: "", label: "전체" },
  { id: "NAT", icon: "🌿", label: "자연" },
  { id: "NIG", icon: "🌙", label: "야경" },
  { id: "ACT", icon: "🚲", label: "자전거" },
  { id: "PHO", icon: "📷", label: "사진" },
  { id: "RES", icon: "🍴", label: "맛집" },
  { id: "CUL", icon: "🏛️", label: "도시탐방" },
];

interface CourseStop {
  name: string;
  address: string;
}

interface Course {
  eyebrow: string;
  title: string;
  duration: string;
  dscr: string;
  stops: CourseStop[];
}

// 코스 자체(제목/설명/경유지)는 고정 콘텐츠라 AI 번역 없이 translation.json에
// 언어별로 미리 넣어두고 가져다 쓴다. themeId(Step2 프리필용 테마 코드)는 번역
// 대상이 아니라 코드값이라 별도로 관리한다.
const THEME_IDS: Record<string, string> = {
  all: "theme1",
  NAT: "theme1",
  NIG: "theme6",
  ACT: "theme4",
  PHO: "theme6",
  RES: "theme3",
  CUL: "theme2",
};

const SejongMatePage = () => {
  const { t } = useTranslation();
  const { isLoggedIn } = useIsLoggedIn();
  const [activeStyle, setActiveStyle] = useUrlState("style", "all");
  const STYLE_FILTERS: FilterOption[] = useMemo(
    () => STYLE_FILTER_IDS.map((option) => ({
      id: option.id,
      label: option.icon ? `${option.icon} ${t(option.label)}` : t(option.label),
    })),
    [t]
  );

  const { data, isLoading, isError } = useMoimList("all", "세종");
  const moims = useMemo(() => data?.pages.flat() ?? [], [data]);

  const filtered = useMemo(() => {
    if (activeStyle === "all") return moims;
    return moims.filter((moim) => moim.cateCd?.split(",").includes(activeStyle));
  }, [moims, activeStyle]);

  const courses = useMemo(
    () => t("sejongMate.courses", { returnObjects: true }) as Record<string, Course>,
    [t]
  );
  const course = courses[activeStyle] ?? courses.all;
  const coursePrefill: MoimCreatePrefill = {
    title: course.title,
    dscr: course.dscr,
    themeId: THEME_IDS[activeStyle] ?? THEME_IDS.all,
    region: "세종",
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
        {isLoggedIn && (
          <Button as={Link} to="/createMoim" state={coursePrefill} text={t("sejongMate.courseCta")} />
        )}
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
                  badge={translateCategoryList(moim.cateNm, t)}
                  imageUrl={moim.imageUrl}
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
