import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "react-router-dom";
import AsyncList from "@/shared/components/asyncList/AsyncList";
import FilterTabs from "@/shared/components/filterTabs/FilterTabs";
import Input from "@/shared/components/input/Input";
import SpotCard from "@/shared/components/spotCard/SpotCard";
import MoimCard from "@/shared/components/moimCard/MoimCard";
import usePlaceList from "@/features/place/hooks/usePlaceList";
import useMoimList from "@/features/moim/hooks/useMoimList";
import { translateCategoryList } from "@/shared/utils/category";
import "./SearchResultPage.css";

const SearchResultPage = () => {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const keyword = searchParams.get("q") ?? "";
  const [tab, setTab] = useState<string>(searchParams.get("tab") === "moim" ? "moim" : "place");
  const [query, setQuery] = useState(keyword);

  const tabs = [
    { id: "place", label: t("search.tabPlace") },
    { id: "moim", label: t("search.tabMoim") },
  ];

  const placeResult = usePlaceList("all", keyword);
  const moimResult = useMoimList("all", keyword);

  const places = useMemo(() => placeResult.data?.pages.flat() ?? [], [placeResult.data]);
  const moims = useMemo(() => moimResult.data?.pages.flat() ?? [], [moimResult.data]);

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) return;
    setSearchParams({ q: trimmed, tab });
  };

  return (
    <>
      <section>
        <form onSubmit={handleSubmit}>
          <Input
            className="search"
            label={t("search.placeholder")}
            placeholder={t("search.placeholder")}
            name="search"
            id="search-result-input"
            blind
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </form>

        <FilterTabs
          options={tabs}
          activeId={tab}
          onChange={(id) => {
            setTab(id as string);
            setSearchParams({ q: keyword, tab: id as string });
          }}
        />
      </section>

      {!keyword ? (
        <p className="search-empty">{t("search.placeholder")}</p>
      ) : tab === "place" ? (
        <ul className="moim-list hr">
          <AsyncList
            isLoading={placeResult.isLoading}
            isError={placeResult.isError}
            data={places}
            renderSkeleton={() => <SpotCard loading />}
            renderItem={(place) => (
              <li key={place.tourId}>
                <Link to={`/place/${place.tourId}`}>
                  <SpotCard
                    imageUrl={place.firstImage || "/images/places/no-image.svg"}
                    title={t(place.tourNm)}
                    place={t(place.roadAddr)}
                    rating={place.avgScore}
                    badge={translateCategoryList(place.cateNm, t)}
                  />
                </Link>
              </li>
            )}
            errorMsg={t("place.errorMsg")}
            emptyMsg={t("search.emptyMsg", { keyword })}
          />
        </ul>
      ) : (
        <ul className="moim-list hr">
          <AsyncList
            isLoading={moimResult.isLoading}
            isError={moimResult.isError}
            data={moims}
            renderSkeleton={() => <MoimCard loading />}
            renderItem={(moim) => (
              <li key={moim.moimId}>
                <Link to={`/moim/${moim.moimId}`}>
                  <MoimCard
                    badge={translateCategoryList(moim.cateNm, t)}
                    imageUrl="/images/places/no-image.svg"
                    title={t(moim.moimTitle)}
                    desc={t(moim.moimDscr)}
                    date={moim.moimStartDt}
                    place={t(moim.region)}
                    member={moim.memberCnt}
                    maxMember={String(moim.maxMember ?? "")}
                    userNm={moim.userNm}
                    userRating={String(moim.avgScore ?? "")}
                  />
                </Link>
              </li>
            )}
            errorMsg={t("moim.errorMsg")}
            emptyMsg={t("search.emptyMsg", { keyword })}
          />
        </ul>
      )}
    </>
  );
};

export default SearchResultPage;
