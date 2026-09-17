import { useRef, useState } from "react";
import { DayPicker, type DateRange } from "react-day-picker";
import { format } from "date-fns";
import { ko, ja } from "date-fns/locale";
import { getApiLang } from "@/shared/utils/lang";
import "react-day-picker/style.css";
import "./DateRangePicker.css"

const DATE_LOCALES = { ko, ja } as const;

interface DateRangePickerProps {
  label: string;
  onChange: (startDate: string, endDate: string) => void;
}

const DateRangePicker = ({ label, onChange }: DateRangePickerProps) => {
  const [dateRange, setDateRange] = useState<DateRange | undefined>();
  const [isOpen, setIsOpen] = useState(false);
  const [activeField, setActiveField] = useState<"start" | "end">();

  const startButtonRef = useRef<HTMLButtonElement>(null);
  const endButtonRef = useRef<HTMLButtonElement>(null);

  const handleSelect = (range: DateRange | undefined) => {
    if (!range) return;

    // 시작일 선택 중
    if (activeField === "start") {
      setDateRange({
        from: range.from,
        to: undefined,
      });

      if (range.from) {
        const startDate = format(range.from, "yyyy-MM-dd");

        onChange(startDate, "");

        setActiveField("end");

        // 종료일 버튼으로 포커스 이동
        endButtonRef.current?.focus();
      }

      return;
    }

    // 종료일 선택 중
    if (activeField === "end") {
      setDateRange({
        from: dateRange?.from,
        to: range.to,
      });

      const startDate = dateRange?.from
        ? format(dateRange.from, "yyyy-MM-dd")
        : "";

      const endDate = range.to
        ? format(range.to, "yyyy-MM-dd")
        : "";

      onChange(startDate, endDate);

      if (range.to) {
        setIsOpen(false);
        setActiveField(undefined);
      }
    }
  };

  const handleStartClick = () => {
    setActiveField("start");
    setIsOpen(true);
  };

  const handleEndClick = () => {
    setActiveField("end");
    setIsOpen(true);
  };

  return (
    <div className="form date-picker">
      <label>{label}</label>

      <div className="date-inputs">
        <button
          ref={startButtonRef}
          type="button"
          className={`btn-datePicker date-start ${activeField === "start" ? "active" : ""}`}
          onClick={handleStartClick}
        >
          {dateRange?.from
            ? format(dateRange.from, "yyyy-MM-dd")
            : "시작일 선택"}
        </button>

        <button
          ref={endButtonRef}
          type="button"
          className={`btn-datePicker date-start ${activeField === "end" ? "active" : ""}`}
          onClick={handleEndClick}
        >
          {dateRange?.to
            ? format(dateRange.to, "yyyy-MM-dd")
            : "종료일 선택"}
        </button>
      </div>

      {isOpen && (
        <DayPicker
          mode="range"
          navLayout="around"
          selected={dateRange}
          onSelect={handleSelect}
          disabled={{ before: new Date() }}
          locale={DATE_LOCALES[getApiLang() as keyof typeof DATE_LOCALES]}
          formatters={{
            formatCaption: (date) => format(date, "yyyy. MM"),
          }}
        />
      )}
    </div>
  );
};

export default DateRangePicker;
