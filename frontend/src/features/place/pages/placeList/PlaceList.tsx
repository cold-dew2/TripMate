import AsyncList from "@/shared/components/asyncList/AsyncList";
import Input from "@/shared/components/input/Input";
import FilterTabs, { type FilterOption } from "@/shared/components/filterTabs/FilterTabs";
import RegionBanner from "@/shared/components/regionBanner/RegionBanner";
import SpotCard from "@/shared/components/spotCard/SpotCard";
import usePlaceList from "../../hooks/usePlaceList";
import { useMemo, useState, useEffect, useRef, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { translateCategoryList } from "@/shared/utils/category";
import useUrlState from "@/shared/hooks/useUrlState";
import "./PlaceList.css";

const PLACE_FILTER_IDS: FilterOption[] = [
  { id: "all", label: "전체" },
  { id: "세종", label: "세종" },
  { id: "서울", label: "서울" },
  { id: "부산", label: "부산" },
  { id: "제주", label: "제주" },
  { id: "OTHER", label: "그외" },
];

// 지역 탭은 카테고리 코드(cateCd)로 필터링할 수 없다(cateCd는 관광지 종류 분류라
// 지역과 무관해 항상 결과가 0건이었다). 대신 백엔드의 전용 region 파라미터를 쓴다
// (도로명주소가 그 지역명으로 "시작"하는 것만 정확히 매칭 — 예전엔 keyword 검색을
// 재사용해서 "세종대로"/"세종대왕" 같은 주소·설명 속 우연한 일치까지 섞여 나왔다).
// "OTHER"는 등록된 지역 어디에도 속하지 않는 관광지를 모아 보여준다.
const REGION_IDS = ["세종", "서울", "부산", "제주", "OTHER"];

const PlaceList = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useUrlState("region", "all");
  const PlaceFilter = useMemo(
    () => PLACE_FILTER_IDS.map((option) => ({ ...option, label: t(option.label) })),
    [t]
  );

  const observerTarget = useRef<HTMLDivElement | null>(null);

  const handleSearchSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) return;
    navigate(`/search?q=${encodeURIComponent(trimmed)}&tab=place`);
  };

  // 1. activeFilter 변경에 따라 새로운 무한 스크롤 query 실행
  const isRegionFilter = REGION_IDS.includes(activeFilter);
  const {
    data,
    isLoading,
    isError,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = usePlaceList("all", undefined, isRegionFilter ? activeFilter : undefined);

  // 2. 다차원 배열로 오는 pages 데이터를 1차원 배열로 평탄화 (flat)
  const places = useMemo(() => {
    return data?.pages.flat() ?? [];
  }, [data]);

  // 3. 스크롤 하단 감지 콜백
  const handleObserver = useCallback(
      (entries: IntersectionObserverEntry[]) => {
        const target = entries[0];
        if (target.isIntersecting && hasNextPage && !isFetchingNextPage) {
          fetchNextPage();
        }
      },
      [hasNextPage, isFetchingNextPage, fetchNextPage]
  );

  // 4. Observer 등록 및 해제
  useEffect(() => {
    const element = observerTarget.current;
    if (!element) return;

    const observer = new IntersectionObserver(handleObserver, { threshold: 0.5 });
    observer.observe(element);

    return () => observer.unobserve(element);
  }, [handleObserver]);

  // 카테고리 선택 처리
  const handleFilterChange = (id: string) => {
    setActiveFilter(id);
  };

  return (
      <>
        <RegionBanner
          compact
          eyebrow={t("home.sejongEyebrow")}
          title={t("home.sejongTitle")}
          href="/search?q=세종&tab=place"
        />
        <section>
          <form onSubmit={handleSearchSubmit} className="place-search-form">
            <Input
                className="search"
                label={t("place.searchLabel")}
                placeholder={t("place.searchPlaceholder")}
                name="search"
                id="search"
                blind
                onChange={(e) => setQuery(e.target.value)}
            />
            <button type="submit" className="place-search-btn" aria-label={t("place.searchLabel")}>🔍</button>
          </form>

          <FilterTabs
              options={PlaceFilter}
              activeId={activeFilter}
              onChange={(id) => handleFilterChange(id as string)}
          />
        </section>

        <section className="hr">
          <ul className="moim-list">
            <AsyncList
                isLoading={isLoading}
                isError={isError}
                data={places}
                renderSkeleton={() => <SpotCard loading />}
                renderItem={(place) => (
                    <li key={place.tourId}>
                      <Link to={`/place/${place.tourId}`}>
                        <SpotCard
                            imageUrl={place.firstImage}
                            title={place.tourNm}
                            place={place.roadAddr}
                            rating={place.avgScore}
                            badge={translateCategoryList(place.cateNm, t)}
                        />
                      </Link>
                    </li>
                )}
                errorMsg={t("place.errorMsg")}
                emptyMsg={t("place.emptyMsg")}
            />
          </ul>

          {/* 무한 스크롤 감지 및 추가 로딩 표시 타겟 */}
          {hasNextPage && (
              <div ref={observerTarget} style={{ minHeight: "40px", padding: "10px 0" }}>
                {isFetchingNextPage && <SpotCard loading />}
              </div>
          )}
        </section>
      </>
  );
};

export default PlaceList;