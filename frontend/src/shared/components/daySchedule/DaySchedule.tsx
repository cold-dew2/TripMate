import { useTranslation } from "react-i18next";
import "./DaySchedule.css";

export interface ScheduleItem {
  id: string;
  time: string;
  placeName: string;
  imageUrl?: string;
}

interface DayScheduleProps {
  day: number;
  date: string;
  items?: ScheduleItem[];
  mode?: "view" | "edit";
  onRemove?: (id: string) => void;
  onAddClick?: () => void;
  onTimeChange?: (id: string, time: string) => void;
}

const HOURS = Array.from({ length: 24 }, (_, hour) => String(hour).padStart(2, "0"));
const MINUTES = ["00", "10", "20", "30", "40", "50"];

interface TimeSelectProps {
  time: string;
  placeName: string;
  onChange: (time: string) => void;
}

const TimeSelect = ({ time, placeName, onChange }: TimeSelectProps) => {
  const { t } = useTranslation();
  const [hour, minute] = time.split(":");
  const safeMinute = MINUTES.includes(minute) ? minute : "00";

  return (
    <span className="schedule-time-picker">
      <select
        className="schedule-time-select"
        value={hour}
        aria-label={t("moimCreate.step3.timeHourLabel", { place: placeName })}
        onChange={(event) => onChange(`${event.target.value}:${safeMinute}`)}
      >
        {HOURS.map((h) => (
          <option key={h} value={h}>{h}</option>
        ))}
      </select>
      <span aria-hidden="true">:</span>
      <select
        className="schedule-time-select"
        value={safeMinute}
        aria-label={t("moimCreate.step3.timeMinuteLabel", { place: placeName })}
        onChange={(event) => onChange(`${hour}:${event.target.value}`)}
      >
        {MINUTES.map((m) => (
          <option key={m} value={m}>{m}</option>
        ))}
      </select>
    </span>
  );
};

const DaySchedule = ({ day, date, items = [], mode = "view", onRemove, onAddClick, onTimeChange }: DayScheduleProps) => {
  const { t } = useTranslation();

  return (
    <div className="daySchedule">
      <div className="schedule-title">DAY {day} ({date})</div>

      {items.length === 0 ? (
        <div className="schedule-empty">
          {mode === "edit" ? t("moimCreate.step3.empty") : t("moimCreate.step3.emptyView")}
        </div>
      ) : (
        <ul className="schedule-items">
          {items.map((item) => (
            <li key={item.id}>
              <span className="schedule-thumb" aria-hidden="true">
                {item.imageUrl && <img src={item.imageUrl} alt="" />}
              </span>
              {mode === "edit" && onTimeChange ? (
                <TimeSelect
                  time={item.time}
                  placeName={item.placeName}
                  onChange={(time) => onTimeChange(item.id, time)}
                />
              ) : (
                <span className="schedule-time">{item.time}</span>
              )}
              <span className="schedule-place">{item.placeName}</span>
              {mode === "edit" && onRemove && (
                <button type="button" className="schedule-remove" onClick={() => onRemove(item.id)} aria-label={t("common.remove")}>
                  ✕
                </button>
              )}
            </li>
          ))}
        </ul>
      )}

      {mode === "edit" && onAddClick && (
        <button type="button" className="schedule-add" onClick={onAddClick}>
          + {t("moimCreate.step3.addSchedule")}
        </button>
      )}
    </div>
  );
};

export default DaySchedule;
