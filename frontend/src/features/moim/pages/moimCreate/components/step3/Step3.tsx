import { useEffect, useMemo, useState, type Dispatch, type SetStateAction } from "react";
import Button from "@/shared/components/button/Button";
import ContentTitle from "@/shared/components/contentTitle/ContentTitle";
import DaySchedule from "@/shared/components/daySchedule/DaySchedule";
import { apiClient } from "@/shared/api/client";
import { addDays, formatMonthDay } from "@/shared/utils/date";
import type { MoimCreateForm } from "@/types/moim";
import type { PlanItem } from "../../MoimCreate";
import type { UseFormSetValue, UseFormWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAlert } from "@/shared/contexts/AlertContext";
import useAiWaitNotice from "@/shared/hooks/useAiWaitNotice";
import { getApiLang } from "@/shared/utils/lang";
import "./Step3.css";

interface Step3Props {
  watch: UseFormWatch<MoimCreateForm>;
  setValue: UseFormSetValue<MoimCreateForm>;
  itemsByDay: Record<number, PlanItem[]>;
  setItemsByDay: Dispatch<SetStateAction<Record<number, PlanItem[]>>>;
  onAddDay: (day: number) => void;
  onPrev: () => void;
  onNext: () => void;
}

// 시간을 바꾼 뒤에는 하루 안에서 다시 시간 순으로 정렬해야 한다(MoimManageDetail의
// 일정 편집과 동일한 규칙).
const sortByTime = (items: PlanItem[]) => [...items].sort((a, b) => a.time.localeCompare(b.time));

interface AiScheduleItem {
  day: number;
  time: string;
  tourId: string;
  tourNm: string;
  firstImage?: string;
  roadAddr?: string;
}

const Step3 = ({ watch, setValue, itemsByDay, setItemsByDay, onAddDay, onPrev, onNext }: Step3Props) => {
  const { t } = useTranslation();
  const { showAlert } = useAlert();
  const moimStartDt = watch("moimStartDt");
  const moimEndDt = watch("moimEndDt");
  const moimCateData = watch("moimCateData");
  const maxMember = watch("maxMember");
  const region = watch("region");

  // 일정은 항상 여행 시작일~종료일(Step2에서 정한 기간)만큼만 존재한다. 예전에는
  // 이와 별개로 "며칠 일정"을 따로 고를 수 있게 해뒀는데, 실제 여행 기간과 어긋나는
  // 일정을 만들 수 있어 혼란스러워서 없애고 여행 기간에서 그대로 계산한다.
  const dayCount = useMemo(() => {
    if (!moimStartDt || !moimEndDt) return 1;
    const diff = Math.round((new Date(moimEndDt).getTime() - new Date(moimStartDt).getTime()) / 86400000) + 1;
    return Math.max(1, diff);
  }, [moimStartDt, moimEndDt]);

  const [isRecommending, setIsRecommending] = useState(false);
  const [recommendError, setRecommendError] = useState<string | null>(null);
  useAiWaitNotice(isRecommending, t("common.aiScheduleWait"));

  useEffect(() => {
    setValue("dayCount", dayCount);
  }, [dayCount, setValue]);

  const removeItem = (day: number, id: string) => {
    setItemsByDay((prev) => ({ ...prev, [day]: (prev[day] ?? []).filter((item) => item.id !== id) }));
  };

  const changeItemTime = (day: number, id: string, time: string) => {
    setItemsByDay((prev) => ({
      ...prev,
      [day]: sortByTime((prev[day] ?? []).map((item) => (item.id === id ? { ...item, time } : item))),
    }));
  };

  const requestAiSchedule = async () => {
    setIsRecommending(true);
    setRecommendError(null);
    try {
      // 이미 사용자가 직접 추가해둔 일정을 함께 보내서, AI가 그 일정은 그대로 두고
      // 비어있는 시간대만 추가로 채우도록 한다.
      const existingItems = Object.entries(itemsByDay).flatMap(([day, items]) =>
        items.map((item) => ({ day: Number(day), time: item.time, tourId: item.tourId }))
      );

      const result = await apiClient.post<{ data: AiScheduleItem[] }>("/tourList/aiSchedule", {
        cateCd: moimCateData?.[0]?.cateCd,
        cateNms: (moimCateData ?? []).map((c) => c.cateNm).filter(Boolean).join(", "),
        region,
        dayCount,
        maxMember,
        moimStartDt,
        moimEndDt,
        existingItems,
        lang: getApiLang(),
      });
      if (!result.success) {
        const message = result.code === "AI_UNAVAILABLE"
          ? t("common.aiUnavailable")
          : t("moimCreate.step3.aiRecommendError");
        showAlert(message);
        setRecommendError(message);
        return;
      }

      if (!result.data.data?.length) {
        showAlert(t("moimCreate.step3.aiRecommendEmpty"));
        setRecommendError(t("moimCreate.step3.aiRecommendEmpty"));
        return;
      }

      // 기존 일정은 그대로 두고 AI가 새로 추천한 항목만 추가한다.
      setItemsByDay((prev) => {
        const next: Record<number, PlanItem[]> = { ...prev };
        result.data.data.forEach((item) => {
          const list = next[item.day] ?? [];
          next[item.day] = [...list, {
            id: `${item.tourId}-${item.day}-${item.time}`,
            time: item.time,
            placeName: item.tourNm,
            tourId: item.tourId,
            imageUrl: item.firstImage,
            roadAddr: item.roadAddr,
          }];
        });
        return next;
      });
    } finally {
      setIsRecommending(false);
    }
  };

  return (
    <div className="create-content">
      <div className="step3-header">
        <ContentTitle title={t("moim.step3.title")} />
      </div>

      {Array.from({ length: dayCount }, (_, index) => index + 1).map((day) => {
        const date = addDays(moimStartDt, day - 1);
        return (
          <DaySchedule
            key={day}
            day={day}
            date={date ? formatMonthDay(date) : `${day}`}
            items={itemsByDay[day] ?? []}
            mode="edit"
            onRemove={(id) => removeItem(day, id)}
            onAddClick={() => onAddDay(day)}
            onTimeChange={(id, time) => changeItemTime(day, id, time)}
          />
        );
      })}

      {recommendError && <p className="step3-ai-error">{recommendError}</p>}

      <div className="buttons fixed step3-fixed">
        <div className="step3-fixed-row">
          <Button text={t("common.previous")} variant="secondary" onClick={onPrev} />
          <Button text={t("common.next")} onClick={onNext} />
        </div>
        <Button
          className="step3-ai-btn"
          text={isRecommending ? t("common.saving") : t("moimCreate.step3.aiRecommend")}
          variant="destructive"
          onClick={requestAiSchedule}
          disabled={isRecommending}
        />
      </div>
    </div>
  );
};

export default Step3;
