import { useState } from "react";
import { useTranslation } from "react-i18next";
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
  defaultStart?: string;
  defaultEnd?: string;
}

const DateRangePicker = ({ label, onChange, defaultStart, defaultEnd }: DateRangePickerProps) => {
  const { t } = useTranslation();
  // 이전 단계로 갔다가 돌아왔을 때(컴포넌트가 다시 mount될 때) 이미 골라둔 날짜가
  // 있으면 그대로 복원한다 — 없으면 기존처럼 빈 상태로 시작한다.
  const [dateRange, setDateRange] = useState<DateRange | undefined>(() =>
    defaultStart ? { from: new Date(defaultStart), to: defaultEnd ? new Date(defaultEnd) : undefined } : undefined
  );
  const [isOpen, setIsOpen] = useState(false);

  // react-day-picker의 range 모드는 이미 선택된 시작일/종료일을 기준으로 새로
  // 클릭한 날짜가 시작일을 바꾸는지 종료일을 바꾸는지 스스로 판단한다(클릭한
  // 날짜가 기존 시작일보다 이르면 시작일을, 그 사이거나 이후면 종료일을 바꾼다).
  // 이 판단과 별도로 "지금 시작일/종료일 중 뭘 고르는 중인지" 상태를 두고 강제로
  // 라우팅하면 오히려 값이 초기화되거나 엉뚱하게 바뀌므로, 라이브러리가 계산한
  // 결과를 그대로 신뢰한다. 단, 아직 아무 것도 선택하지 않은 첫 클릭만은
  // 라이브러리 기본값(from=to=클릭한 날짜)대로 두면 곧장 range가 완성돼 버려
  // 종료일을 고를 기회 없이 캘린더가 닫히므로, 그때만 종료일을 비워둔다.
  const handleSelect = (range: DateRange | undefined) => {
    const isFirstPick = !dateRange?.from;
    const nextRange = isFirstPick && range?.from ? { from: range.from, to: undefined } : range;

    setDateRange(nextRange);
    onChange(
      nextRange?.from ? format(nextRange.from, "yyyy-MM-dd") : "",
      nextRange?.to ? format(nextRange.to, "yyyy-MM-dd") : ""
    );

    if (nextRange?.from && nextRange?.to) {
      setIsOpen(false);
    }
  };

  return (
    <div className="form date-picker">
      <label>{label}</label>

      <div className="date-inputs">
        <button
          type="button"
          className="btn-datePicker date-start"
          onClick={() => setIsOpen(true)}
        >
          {dateRange?.from ? format(dateRange.from, "yyyy-MM-dd") : t('common.startDateSelect')}
        </button>

        <button
          type="button"
          className="btn-datePicker date-start"
          onClick={() => setIsOpen(true)}
        >
          {dateRange?.to ? format(dateRange.to, "yyyy-MM-dd") : t('common.endDateSelect')}
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
