import AsyncList from "@/shared/components/asyncList/AsyncList";
import Input from "@/shared/components/input/Input"
import FilterTabs, { type FilterOption } from "@/shared/components/filterTabs/FilterTabs";
import SpotCard from "@/shared/components/spotCard/SpotCard";
import usePlaceList from "../../hooks/usePlaceList";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";


const PlaceFilter: FilterOption[] = [
  { id: "all", label: "전체", },
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

  const handleSearchSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) return;
    navigate(`/moimSearch?q=${encodeURIComponent(trimmed)}`)
  }

  const { data: places = [], isLoading, isError } = usePlaceList();

  const filterPlace = useMemo(() => {
    return places.filter((place) => {
      if (activeFilter === "all") return true;
      return place.cateNm === activeFilter;
    });
  }, [places, activeFilter]);

  return (
    <>
      <section>
        <form onSubmit={handleSearchSubmit}>
          <Input className="search" label={t("place.searchLabel")} placeholder={t("place.searchPlaceholder")} name="search" id="search" blind onChange={(e) => setQuery(e.target.value)} />
        </form>

        <FilterTabs
          options={PlaceFilter}
          activeId={activeFilter}
          onChange={(id) => setActiveFilter(id as string)}
        />
      </section>

      <section className="hr">
        <ul className="moim-list">
          <AsyncList
            isLoading={isLoading}
            isError={isError}
            data={filterPlace}
            renderSkeleton={() => <SpotCard loading />}
            renderItem={(place) => (
              <li key={place.tourId}>
                <Link to={`/place/${place.tourId}`}>
                  <SpotCard 
                    imageUrl={`/images/places/${place.tourId}.jpeg`} 
                    title={t(place.tourNm)} 
                    place={t(place.roadAddr)}
                    rating={place.avgScore} 
                    badge={t(place.cateNm)} />
                </Link>
              </li>
            )}
            errorMsg={t("place.errorMsg")}
            emptyMsg={t("place.emptyMsg")}
          />
        </ul>
      </section>
    </>
  )
}

export default PlaceList