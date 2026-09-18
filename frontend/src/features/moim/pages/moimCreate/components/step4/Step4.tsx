import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import Button from "@/shared/components/button/Button";
import Input from "@/shared/components/input/Input";
import FilterTabs from "@/shared/components/filterTabs/FilterTabs";
import { apiClient } from "@/shared/api/client";
import type { MoimCreateForm } from "@/types/moim";
import type { Place } from "@/types/place";
import type { PlanItem } from "../../MoimCreate";
import type { UseFormWatch } from "react-hook-form";
import "./Step4.css";

const isMock = (import.meta.env.VITE_API_BASE_URL ?? "").startsWith("/data");

interface Step4Props {
  day: number;
  watch: UseFormWatch<MoimCreateForm>;
  items: PlanItem[];
  onAddItem: (item: PlanItem) => void;
  onDone: () => void;
}

type TabId = "all" | "recommend" | "custom";

const nextTime = (count: number) => {
  const hour = (10 + count * 3) % 24;
  return `${String(hour).padStart(2, "0")}:00`;
};

const Step4 = ({ day, watch, items, onAddItem, onDone }: Step4Props) => {
  const { t } = useTranslation();
  const moimCateData = watch("moimCateData");
  const region = watch("region");
  const cateCodes = useMemo(() => (moimCateData ?? []).map((c) => c.cateCd), [moimCateData]);

  const [activeTab, setActiveTab] = useState<TabId>("all");
  const [keyword, setKeyword] = useState("");
  const [results, setResults] = useState<Place[]>([]);
  const [customSpots, setCustomSpots] = useState<Place[]>([]);
  const [customName, setCustomName] = useState("");
  const [customAddr, setCustomAddr] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [addedIds, setAddedIds] = useState<string[]>([]);

  useEffect(() => {
    // 검색어를 직접 입력하지 않은 기본 목록은 Step2에서 고른 지역으로 좁혀서 보여준다.
    // (검색어를 입력하면 그 검색어를 우선한다 — 백엔드가 키워드 하나만 받기 때문에 동시 적용은 안 됨)
    const searchKeyword = keyword.trim() || region || "";

    const timer = window.setTimeout(async () => {
      const result = await apiClient.get<{ data: Place[] }>("/tourList/tourSearch", { page: 1, keyword: searchKeyword });
      if (!result.success) return;

      let places = result.data.data ?? [];
      if (isMock && searchKeyword) {
        const lower = searchKeyword.toLowerCase();
        places = places.filter((place) =>
          place.tourNm?.toLowerCase().includes(lower)
          || place.roadAddr?.toLowerCase().includes(lower)
          || place.sidoNm?.toLowerCase().includes(lower)
          || place.sggNm?.toLowerCase().includes(lower)
        );
      }
      setResults(places);
    }, 300);
    return () => window.clearTimeout(timer);
  }, [keyword, region]);

  const tabs = [
    { id: "all", label: t("moimCreate.step4.tabAll") },
    { id: "recommend", label: t("moimCreate.step4.tabRecommend") },
    { id: "custom", label: t("moimCreate.step4.tabCustom") },
  ];

  const displayedList: Place[] = activeTab === "custom"
    ? customSpots
    : activeTab === "recommend"
      ? results.filter((place) => cateCodes.length === 0 || cateCodes.includes(place.cateCd))
      : results;

  const handleAdd = (place: Place) => {
    onAddItem({
      id: `${place.tourId}-${Date.now()}`,
      time: nextTime(items.length + addedIds.length),
      placeName: place.tourNm,
      tourId: place.tourId,
      imageUrl: place.firstImage,
      sidoNm: place.sidoNm,
      sggNm: place.sggNm,
      roadAddr: place.roadAddr,
    });
    setAddedIds((prev) => [...prev, place.tourId]);
  };

  const handleSaveCustom = async () => {
    if (!customName.trim() || !customAddr.trim()) return;
    setIsSaving(true);
    try {
      let tourId: string;
      if (isMock) {
        tourId = `MOCK${Date.now()}`;
      } else {
        const result = await apiClient.post<{ data: { tourId: string } }>(
          "/tourList/customTour",
          { tourNm: customName.trim(), roadAddr: customAddr.trim() }
        );
        if (!result.success) return;
        tourId = result.data.data.tourId;
      }

      const created: Place = {
        tourId,
        tourNm: customName.trim(),
        sidoCd: "", sidoNm: "", sggCd: "", sggNm: "", emdCd: "", emdNm: "",
        roadAddr: customAddr.trim(),
        detailAddr: "", zipCd: "", cateCd: "", cateNm: t("moimCreate.step4.customCateNm"),
        avgScore: 0,
      };
      setCustomSpots((prev) => [created, ...prev]);
      setActiveTab("custom");
      setCustomName("");
      setCustomAddr("");
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="create-content step4-content">
      <p className="step4-day-label">{t("moimCreate.step4.dayLabel", { day })}</p>
      <div className="step4-search">
        <input
          type="text"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder={t("moimCreate.step4.searchPlaceholder")}
          aria-label={t("moimCreate.step4.searchPlaceholder")}
        />
      </div>

      <FilterTabs options={tabs} activeId={activeTab} onChange={(id) => setActiveTab(id as TabId)} />

      {displayedList.length === 0 ? (
        <p className="step4-empty">
          {activeTab === "custom" ? t("moimCreate.step4.emptyCustom") : t("moimCreate.step4.empty")}
        </p>
      ) : (
        <ul className="step4-list">
          {displayedList.map((place) => {
            const isAdded = addedIds.includes(place.tourId);
            return (
              <li key={place.tourId}>
                <span className="step4-swatch" aria-hidden="true">
                  {place.firstImage && <img src={place.firstImage} alt="" />}
                </span>
                <span className="step4-info">
                  <strong>{place.tourNm}</strong>
                  <span className="step4-cate">{place.cateNm || t("moimCreate.step4.customCateNm")}</span>
                </span>
                <button
                  type="button"
                  className={`step4-add-btn ${isAdded ? "is-added" : ""}`}
                  onClick={() => handleAdd(place)}
                  aria-label={t("moimCreate.step3.addSchedule")}
                >
                  {isAdded ? "✓" : "+"}
                </button>
              </li>
            );
          })}
        </ul>
      )}

      <div className="step4-custom">
        <p className="step4-custom-title">{t("moimCreate.step4.directInput")}</p>
        <Input
          label={t("moimCreate.step4.nameInputPlaceholder")}
          blind
          name="customTourNm"
          placeholder={t("moimCreate.step4.nameInputPlaceholder")}
          value={customName}
          onChange={(event) => setCustomName(event.target.value)}
        />
        <Input
          label={t("moimCreate.step4.addressInputPlaceholder")}
          blind
          name="customTourAddr"
          placeholder={t("moimCreate.step4.addressInputPlaceholder")}
          value={customAddr}
          onChange={(event) => setCustomAddr(event.target.value)}
        />
        <Button
          text={isSaving ? t("common.saving") : t("moimCreate.step4.save")}
          variant="secondary"
          onClick={handleSaveCustom}
          disabled={isSaving || !customName.trim() || !customAddr.trim()}
        />
      </div>

      <div className="buttons fixed">
        <Button text={t("common.previous")} variant="secondary" onClick={onDone} />
        <Button text={t("common.next")} onClick={onDone} />
      </div>
    </div>
  );
};

export default Step4;
