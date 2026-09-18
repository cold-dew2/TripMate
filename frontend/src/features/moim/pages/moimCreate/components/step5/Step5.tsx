import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import Button from "@/shared/components/button/Button";
import TransportLegView from "@/shared/components/transportLeg/TransportLegView";
import ItineraryMap from "@/shared/components/itineraryMap/ItineraryMap";
import { addDays, formatMonthDay } from "@/shared/utils/date";
import { useTransportRecommend, toTransportStops, type TransportLeg } from "../../../../hooks/useTransportRecommend";
import type { MoimCreateForm } from "@/types/moim";
import type { PlanItem } from "../../MoimCreate";
import type { UseFormWatch } from "react-hook-form";
import { useAlert } from "@/shared/contexts/AlertContext";
import "./Step5.css";

interface Step5Props {
  watch: UseFormWatch<MoimCreateForm>;
  itemsByDay: Record<number, PlanItem[]>;
  onEditPlan: () => void;
  onNext: () => void;
}

const Step5 = ({ watch, itemsByDay, onEditPlan, onNext }: Step5Props) => {
  const { t } = useTranslation();
  const { showAlert } = useAlert();
  const moimStartDt = watch("moimStartDt");
  const moimEndDt = watch("moimEndDt");
  const dayCount = watch("dayCount") ?? Object.keys(itemsByDay).length ?? 1;
  // itemsByDay는 실제로 일정을 추가한 날짜만 키로 가지므로, 하루도 추가하지 않은
  // 빈 날짜까지 빠짐없이 보여주기 위해 1~dayCount 전체를 순회한다.
  const days = Array.from({ length: dayCount }, (_, index) => index + 1);

  const range = moimStartDt && moimEndDt
    ? `${moimStartDt.replaceAll("-", ".")} - ${formatMonthDay(moimEndDt)}`
    : "";

  const transportRecommend = useTransportRecommend();
  const legsByKey = useMemo(() => {
    const map = new Map<string, TransportLeg>();
    (transportRecommend.data ?? []).forEach((leg) => {
      map.set(`${leg.day}-${leg.fromTourId}-${leg.toTourId}`, leg);
    });
    return map;
  }, [transportRecommend.data]);

  const totalStops = days.reduce((sum, day) => sum + (itemsByDay[day]?.length ?? 0), 0);
  const mapStops = days.flatMap((day) => itemsByDay[day] ?? []);

  return (
    <div className="create-content step5-content">
      <div className="step5-map">
        <ItineraryMap stops={mapStops} />
      </div>

      <p className="step5-flow-title">
        {t("moimCreate.step5.flowTitle", { count: days.length, range })}
      </p>

      {totalStops >= 2 && (
        <div className="step5-transport-trigger">
          <Button
            text={transportRecommend.isPending ? t("common.saving") : t("moimCreate.step5.transportRecommend")}
            variant="secondary"
            onClick={() => transportRecommend.mutate(toTransportStops(itemsByDay), {
              onError: (error) => {
                if ((error as { code?: string } | null)?.code === "AI_UNAVAILABLE") {
                  showAlert(t("common.aiUnavailable"));
                }
              },
            })}
            disabled={transportRecommend.isPending}
          />
          {transportRecommend.isError && (
            <p className="step5-transport-error">
              {(transportRecommend.error as { code?: string } | null)?.code === "AI_UNAVAILABLE"
                ? t("common.aiUnavailable")
                : t("moimCreate.step5.transportRecommendError")}
            </p>
          )}
        </div>
      )}

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
                {items.map((item, index) => {
                  const prev = items[index - 1];
                  const leg = prev ? legsByKey.get(`${day}-${prev.tourId}-${item.tourId}`) : undefined;
                  return (
                    <li key={item.id}>
                      {leg && <TransportLegView leg={leg} />}
                      <div className="step5-item-row">
                        <span className="step5-time">{item.time}</span>
                        <span className="step5-place">{item.placeName}</span>
                      </div>
                    </li>
                  );
                })}
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
