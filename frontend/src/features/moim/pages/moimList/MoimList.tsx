import AsyncList from "@/shared/components/asyncList/AsyncList";
import Input from "@/shared/components/input/Input";
import MoimCard from "@/shared/components/moimCard/MoimCard";
import useMoimList from "../../hooks/useMoimList";
import { useMemo, useState, useEffect, useRef, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import FilterTabs, { type FilterOption } from "@/shared/components/filterTabs/FilterTabs";
import Button from "@/shared/components/button/Button";
import RegionBanner from "@/shared/components/regionBanner/RegionBanner";
import { resolveImageUrl } from "@/shared/utils/url";

const MoimFilter: FilterOption[] = [
  { id: "all", label: "전체" },
  { id: "세종", label: "세종" },
  { id: "서울", label: "서울" },
  { id: "부산", label: "부산" },
  { id: "제주", label: "제주" },
];

// 소모임은 지역 컬럼이 따로 없어 제목/소개 텍스트로 지역 탭을 매칭한다.
// (cateCd로 보내면 실제 카테고리 코드와 일치하지 않아 항상 결과가 0건이 되는 버그였음)
const REGION_IDS = ["세종", "서울", "부산", "제주"];

const MoimList = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<string>("all");

  const observerTarget = useRef<HTMLDivElement | null>(null);

  const handleSearchSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) return;
    navigate(`/search?q=${encodeURIComponent(trimmed)}&tab=moim`);
  };

  // 1. useInfiniteQuery 지원 Hook 호출
  const isRegionFilter = REGION_IDS.includes(activeFilter);
  const {
    data,
    isLoading,
    isError,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useMoimList(isRegionFilter ? "all" : activeFilter, isRegionFilter ? activeFilter : undefined);

  // 2. data.pages flat() 처리 (filter.isNotAFunction 에러 방지)
  const moims = useMemo(() => {
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

  // 4. Observer 관찰 등록 및 해제
  useEffect(() => {
    const element = observerTarget.current;
    if (!element) return;

    const observer = new IntersectionObserver(handleObserver, { threshold: 0.5 });
    observer.observe(element);

    return () => observer.unobserve(element);
  }, [handleObserver]);

  return (
      <>
        <RegionBanner
          compact
          eyebrow={t("home.sejongEyebrow")}
          title={t("home.sejongMoimTitle")}
          href="/moimList/sejong"
        />
        <section>
          <form onSubmit={handleSearchSubmit}>
            <Input
                className="search"
                label={t("home.searchLabel")}
                placeholder={t("moim.searchPlaceholder")}
                name="search"
                id="search"
                blind
                onChange={(e) => setQuery(e.target.value)}
            />
          </form>

          <FilterTabs
              options={MoimFilter}
              activeId={activeFilter}
              onChange={(id) => setActiveFilter(id as string)}
          />
        </section>

        <section className="hr">
          <ul className="moim-list">
            <AsyncList
                isLoading={isLoading}
                isError={isError}
                data={moims}
                renderSkeleton={() => <MoimCard loading />}
                renderItem={(moim) => (
                    <li key={moim.moimId}>
                      <Link to={`/moim/${moim.moimId}`}>
                        <MoimCard
                            badge={moim.cateNm}
                            imageUrl={resolveImageUrl(moim.imageUrl)}
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
                emptyMsg={t("moim.emptyMsg")}
            />
          </ul>

          {/* 무한 스크롤 타겟 및 로딩 감지 */}
          {hasNextPage && (
              <div ref={observerTarget} style={{ minHeight: "40px", padding: "10px 0" }}>
                {isFetchingNextPage && <MoimCard loading />}
              </div>
          )}
        </section>

      <Button text={t("moim.button")} variant="fixed" as="a" href="/createMoim" />
      </>
  );
};

export default MoimList;
