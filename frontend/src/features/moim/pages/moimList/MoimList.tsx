import AsyncList from "@/shared/components/asyncList/AsyncList";
import Input from "@/shared/components/input/Input"
import MoimCard from "@/shared/components/moimCard/MoimCard";
import useMoimList from "../../hooks/useMoimList";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import FilterTabs, { type FilterOption } from "@/shared/components/filterTabs/FilterTabs";
import Button from "@/shared/components/button/Button";


const MoimFilter: FilterOption[] = [
  { id: "all", label: "전체", },
  { id: "서울", label: "서울"},
  { id: "부산", label: "부산"},
  { id: "제주", label: "제주"},
  { id: "foreignWelcome", label: "외국인환영"},
];


const MoimList = () => {
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

  const { data: moims = [], isLoading, isError } = useMoimList();

  const filteredMoims = useMemo(() => {
    return moims.filter((moim) => {
      if (activeFilter === "all") return true;
      return moim.cateNm === activeFilter;
    });
  }, [moims, activeFilter]);

  return (
    <>
      <section>
        <form onSubmit={handleSearchSubmit}>
          <Input className="search" label={t("home.searchLabel")} placeholder={t("moim.searchPlaceholder")} name="search" id="search" blind onChange={(e) => setQuery(e.target.value)} />
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
            data={filteredMoims}
            renderSkeleton={() => <MoimCard loading />}
            renderItem={(moim) => (
              <li key={moim.moimId}>
                <Link to={`/moim/${moim.moimId}`}>
                  <MoimCard
                    badge={moim.cateNm}
                    imageUrl={moim.imageUrl}
                    title={t(moim.moimTitle)}
                    desc={t(moim.moimDscr)}
                    date={moim.moimStartDt}
                    place={t(moim.region)}
                    member={moim.memberCnt}
                    userNm={moim.userNm}
                    userRating={moim.userRating}
                  />
                </Link>
              </li>
            )}
            errorMsg={t("moim.errorMsg")}
            emptyMsg={t("moim.emptyMsg")}
          />
        </ul>
      </section>

      <Button text={t("moim.button")} variant="fixed" as="a" href="/"/>
    </>
  )
}

export default MoimList