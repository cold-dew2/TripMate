import { useTranslation } from "react-i18next";
import Button from "@/shared/components/button/Button";
import { addDays, formatMonthDay } from "@/shared/utils/date";
import type { MoimCreateForm } from "@/types/moim";
import type { PlanItem } from "../../MoimCreate";
import type { UseFormWatch } from "react-hook-form";
import "./Step5.css";

interface Step5Props {
  watch: UseFormWatch<MoimCreateForm>;
  itemsByDay: Record<number, PlanItem[]>;
  onEditPlan: () => void;
  onNext: () => void;
}

const Step5 = ({ watch, itemsByDay, onEditPlan, onNext }: Step5Props) => {
  const { t } = useTranslation();
  const moimStartDt = watch("moimStartDt");
  const moimEndDt = watch("moimEndDt");
  const days = Object.keys(itemsByDay).map(Number).sort((a, b) => a - b);

  const range = moimStartDt && moimEndDt
    ? `${moimStartDt.replaceAll("-", ".")} - ${formatMonthDay(moimEndDt)}`
    : "";

  return (
    <div className="create-content step5-content">
      <div className="step5-map">
        <span>{t("moimCreate.step5.mapPreview")}</span>
      </div>

      <p className="step5-flow-title">
        {t("moimCreate.step5.flowTitle", { count: days.length, range })}
      </p>

      {days.map((day) => {
        const date = addDays(moimStartDt, day - 1);
        const items = itemsByDay[day] ?? [];
        return (
          <div key={day} className="step5-day">
            <p className="step5-day-title">
              {t("moimCreate.step5.dayLabel", { day, date: date ? formatMonthDay(date) : day })}
            </p>
            {items.length === 0 ? (
              <p className="step5-empty">{t("moim.step3.emptyView")}</p>
            ) : (
              <ul className="step5-items">
                {items.map((item) => (
                  <li key={item.id}>
                    <span className="step5-time">{item.time}</span>
                    <span className="step5-place">{item.placeName}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        );
      })}

      <div className="buttons fixed">
        <Button text={t("moimCreate.step5.editPlan")} variant="secondary" onClick={onEditPlan} />
        <Button text={t("common.next")} onClick={onNext} />
      </div>
    </div>
  );
};

export default Step5;
