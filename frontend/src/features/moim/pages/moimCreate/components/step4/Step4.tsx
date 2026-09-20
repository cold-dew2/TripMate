import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import Button from "@/shared/components/button/Button";
import Input from "@/shared/components/input/Input";
import FilterTabs from "@/shared/components/filterTabs/FilterTabs";
import { apiClient } from "@/shared/api/client";
import { getApiLang } from "@/shared/utils/lang";
import { useAlert } from "@/shared/contexts/AlertContext";
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
  onRemoveItem: (tourId: string) => void;
  onDone: () => void;
}

type TabId = "all" | "recommend" | "custom";

const nextTime = (count: number) => {
  const hour = (10 + count * 3) % 24;
  return `${String(hour).padStart(2, "0")}:00`;
};

const Step4 = ({ day, watch, items, onAddItem, onRemoveItem, onDone }: Step4Props) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { showAlert } = useAlert();
  const moimCateData = watch("moimCateData");
  const region = watch("region");
  const cateCodes = useMemo(() => (moimCateData ?? []).map((c) => c.cateCd), [moimCateData]);

  const [activeTab, setActiveTab] = useState<TabId>("all");
  const [keyword, setKeyword] = useState("");
  const [results, setResults] = useState<Place[]>([]);
  const [recommendResults, setRecommendResults] = useState<Place[]>([]);
  const [customSpots, setCustomSpots] = useState<Place[]>([]);
  const [customName, setCustomName] = useState("");
  const [customAddr, setCustomAddr] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [isSearching, setIsSearching] = useState(true);
  // 테마를 아직 안 골랐으면 애초에 추천 목록을 불러올 게 없으니 기본값은 false로 둔다
  // (true로 두면 "추천 관광지" 탭에서 영원히 "검색 중..."만 보이게 된다).
  const [isRecommendLoading, setIsRecommendLoading] = useState(false);

  const runSearch = async (searchKeyword: string) => {
    setIsSearching(true);
    try {
      const result = await apiClient.get<{ data: Place[] }>("/tourList/tourSearch", { page: 1, keyword: searchKeyword, lang: getApiLang() });
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
    } finally {
      setIsSearching(false);
    }
  };

  useEffect(() => {
    // 검색어를 직접 입력하지 않은 기본 목록은 Step2에서 고른 지역으로 좁혀서 보여준다.
    // (검색어를 입력하면 그 검색어를 우선한다 — 백엔드가 키워드 하나만 받기 때문에 동시 적용은 안 됨)
    const searchKeyword = keyword.trim() || region || "";
    const timer = window.setTimeout(() => void runSearch(searchKeyword), 300);
    return () => window.clearTimeout(timer);
  }, [keyword, region]);

  useEffect(() => {
    // 관광지는 관광공사 API 분류(TOUR_LCLS_CD) 코드를 쓰고 모임 테마는 이 앱의
    // 자체 분류(CATE_ID) 코드를 쓰기 때문에 cateCd끼리는 서로 비교할 수 없다.
    // 대신 선택한 테마의 한글 이름(예: 해변, 카페)을 검색 키워드로 사용해
    // 이름/설명/주소에 그 단어가 포함된 관광지를 추천 목록으로 보여준다.
    let cancelled = false;
    (async () => {
      const cateNames = Array.from(new Set((moimCateData ?? []).map((c) => c.cateNm).filter(Boolean))).slice(0, 5) as string[];
      if (cateNames.length === 0) {
        setRecommendResults([]);
        return;
      }

      setIsRecommendLoading(true);
      const lists = await Promise.all(
        cateNames.map(async (name) => {
          const result = await apiClient.get<{ data: Place[] }>("/tourList/tourSearch", { page: 1, keyword: name, lang: getApiLang() });
          return result.success ? result.data.data ?? [] : [];
        })
      );
      if (cancelled) return;

      const merged = new Map<string, Place>();
      lists.flat().forEach((place) => merged.set(place.tourId, place));
      setRecommendResults(Array.from(merged.values()).sort((a, b) => (b.avgScore ?? 0) - (a.avgScore ?? 0)));
      setIsRecommendLoading(false);
    })();

    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cateCodes.join(",")]);

  const tabs = [
    { id: "all", label: t("moimCreate.step4.tabAll") },
    { id: "recommend", label: t("moimCreate.step4.tabRecommend") },
    { id: "custom", label: t("moimCreate.step4.tabCustom") },
  ];

  const displayedList: Place[] = activeTab === "custom"
    ? customSpots
    : activeTab === "recommend"
      ? recommendResults
      : results;

  // items(현재 날짜에 이미 추가된 일정)를 그대로 "추가됨" 여부의 기준으로 삼는다.
  // 예전에는 별도의 addedIds 상태로 따로 추적해서, 다시 누르면 취소가 아니라 매번
  // 새로 추가돼 같은 관광지가 여러 건 쌓이는 문제가 있었다.
  const toggleItem = (place: Place) => {
    const existing = items.find((item) => item.tourId === place.tourId);
    if (existing) {
      onRemoveItem(place.tourId);
      return;
    }
    onAddItem({
      id: `${place.tourId}-${Date.now()}`,
      time: nextTime(items.length),
      placeName: place.tourNm,
      tourId: place.tourId,
      imageUrl: place.firstImage,
      sidoNm: place.sidoNm,
      sggNm: place.sggNm,
      roadAddr: place.roadAddr,
    });
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
        if (!result.success) {
          // 이 요청은 로그인해야만 되는데, 실패해도 아무 반응이 없으면 "왜 등록이
          // 안 되는지" 알 수 없으니 원인을 그대로 보여준다.
          if (result.code === "NEED_LOGIN") {
            showAlert(t("moimCreate.step4.customTourNeedLogin"));
            navigate("/auth");
          } else {
            showAlert(result.message || t("moimCreate.step4.customTourError"));
          }
          return;
        }
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
          onKeyDown={(event) => {
            // 이 input이 소모임 등록 전체를 감싸는 <form> 안에 있어서, 엔터를 막지
            // 않으면 검색이 아니라 그 상위 form이 그대로 제출(=모임 등록)돼 버린다.
            if (event.key === "Enter") {
              event.preventDefault();
              void runSearch(keyword.trim() || region || "");
            }
          }}
          placeholder={t("moimCreate.step4.searchPlaceholder")}
          aria-label={t("moimCreate.step4.searchPlaceholder")}
        />
      </div>

      <FilterTabs options={tabs} activeId={activeTab} onChange={(id) => setActiveTab(id as TabId)} />

      {(activeTab === "all" && isSearching) || (activeTab === "recommend" && isRecommendLoading) ? (
        <p className="step4-empty">{t("moimCreate.step4.searching")}</p>
      ) : displayedList.length === 0 ? (
        <p className="step4-empty">
          {activeTab === "custom" ? t("moimCreate.step4.emptyCustom") : t("moimCreate.step4.empty")}
        </p>
      ) : (
        <ul className="step4-list">
          {displayedList.map((place) => {
            const isAdded = items.some((item) => item.tourId === place.tourId);
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
                  onClick={() => toggleItem(place)}
                  aria-label={isAdded ? t("moimCreate.step4.removeSchedule") : t("moimCreate.step3.addSchedule")}
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
