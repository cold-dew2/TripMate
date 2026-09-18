import { useEffect, useMemo, useState, type Dispatch, type SetStateAction } from "react";
import Button from "@/shared/components/button/Button";
import ContentTitle from "@/shared/components/contentTitle/ContentTitle";
import Select from "@/shared/components/select/Select";
import DaySchedule from "@/shared/components/daySchedule/DaySchedule";
import { apiClient } from "@/shared/api/client";
import { addDays, formatMonthDay } from "@/shared/utils/date";
import type { MoimCreateForm } from "@/types/moim";
import type { PlanItem } from "../../MoimCreate";
import type { UseFormSetValue, UseFormWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
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
  const moimStartDt = watch("moimStartDt");
  const moimEndDt = watch("moimEndDt");
  const moimCateData = watch("moimCateData");
  const maxMember = watch("maxMember");
  const region = watch("region");

  const maxDays = useMemo(() => {
    if (!moimStartDt || !moimEndDt) return 1;
    const diff = Math.round((new Date(moimEndDt).getTime() - new Date(moimStartDt).getTime()) / 86400000) + 1;
    return Math.max(1, diff);
  }, [moimStartDt, moimEndDt]);

  const [dayCount, setDayCount] = useState(maxDays);
  const [isRecommending, setIsRecommending] = useState(false);
  const [recommendError, setRecommendError] = useState<string | null>(null);

  useEffect(() => {
    setDayCount(maxDays);
    setValue("dayCount", maxDays);
  }, [maxDays, setValue]);

  const dayOptions = Array.from({ length: 10 }, (_, index) => {
    const count = index + 1;
    return {
      value: String(count),
      option: count === 1
        ? t("moimCreate.step3.daysOptionOne")
        : t("moimCreate.step3.daysOptionMulti", { count, nights: count - 1 }),
    };
  });

  const removeItem = (day: number, id: string) => {
    setItemsByDay((prev) => ({ ...prev, [day]: (prev[day] ?? []).filter((item) => item.id !== id) }));
  };

  const changeItemTime = (day: number, id: string, time: string) => {
    setItemsByDay((prev) => ({
      ...prev,
      [day]: (prev[day] ?? []).map((item) => (item.id === id ? { ...item, time } : item)),
    }));
  };

  const requestAiSchedule = async () => {
    setIsRecommending(true);
    setRecommendError(null);
    try {
      const result = await apiClient.post<{ data: AiScheduleItem[] }>("/tourList/aiSchedule", {
        cateCd: moimCateData?.[0]?.cateCd,
        cateNms: (moimCateData ?? []).map((c) => c.cateNm).filter(Boolean).join(", "),
        keyword: region,
        dayCount,
        maxMember,
        moimStartDt,
        moimEndDt,
      });
      if (!result.success) {
        setRecommendError(t("moimCreate.step3.aiRecommendError"));
        return;
      }

      const grouped: Record<number, PlanItem[]> = {};
      result.data.data?.forEach((item) => {
        const list = grouped[item.day] ?? [];
        list.push({
          id: `${item.tourId}-${item.day}-${item.time}`,
          time: item.time,
          placeName: item.tourNm,
          tourId: item.tourId,
          imageUrl: item.firstImage,
          roadAddr: item.roadAddr,
        });
        grouped[item.day] = list;
      });

      if (Object.keys(grouped).length === 0) {
        setRecommendError(t("moimCreate.step3.aiRecommendEmpty"));
        return;
      }

      setItemsByDay(grouped);
    } finally {
      setIsRecommending(false);
    }
  };

  return (
    <div className="create-content">
      <div className="step3-header">
        <ContentTitle title={t("moim.step3.title")} />
        <Select
          label={t("moimCreate.step3.daysLabel")}
          blind
          id="dayCount"
          name="dayCount"
          value={String(dayCount)}
          options={dayOptions}
          onChange={(event) => {
            const next = Number(event.target.value);
            setDayCount(next);
            setValue("dayCount", next);
          }}
        />
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
