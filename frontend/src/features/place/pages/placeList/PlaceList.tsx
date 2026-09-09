import AsyncList from "@/shared/components/asyncList/AsyncList";
import Input from "@/shared/components/input/Input";
import FilterTabs, { type FilterOption } from "@/shared/components/filterTabs/FilterTabs";
import SpotCard from "@/shared/components/spotCard/SpotCard";
import usePlaceList from "../../hooks/usePlaceList";
import { useMemo, useState, useEffect, useRef, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";

const PlaceFilter: FilterOption[] = [
  { id: "all", label: "전체" },
  { id: "서울", label: "서울" },
  { id: "부산", label: "부산" },
  { id: "제주", label: "제주" },
  { id: "foreignWelcome", label: "외국인환영" },
];

const PlaceList = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<string>("all");

  const observerTarget = useRef<HTMLDivElement | null>(null);

  const handleSearchSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) return;
    navigate(`/moimSearch?q=${encodeURIComponent(trimmed)}`);
  };

  // 1. activeFilter 변경에 따라 새로운 무한 스크롤 query 실행
  const {
    data,
    isLoading,
    isError,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = usePlaceList(activeFilter);

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
        <section>
          <form onSubmit={handleSearchSubmit}>
            <Input
                className="search"
                label={t("place.searchLabel")}
                placeholder={t("place.searchPlaceholder")}
                name="search"
                id="search"
                blind
                onChange={(e) => setQuery(e.target.value)}
            />
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
                            imageUrl={place.firstImage || `/images/places/no-image.png`}
                            title={t(place.tourNm)}
                            place={t(place.roadAddr)}
                            rating={place.avgScore}
                            badge={t(place.cateNm)}
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