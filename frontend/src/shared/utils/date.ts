import { getApiLang } from "./lang";

// 요일 약어가 한국어로 고정돼 있어서, 영어/일본어 화면에서도 날짜 옆에 "(월)"처럼
// 한국어 요일이 그대로 붙어 나오는 문제가 있었다. 화면 언어에 맞는 요일로 바꾼다.
const DOW_BY_LANG: Record<string, string[]> = {
  ko: ["일", "월", "화", "수", "목", "금", "토"],
  en: ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"],
  ja: ["日", "月", "火", "水", "木", "金", "土"],
};
const dowLabel = (day: number) => (DOW_BY_LANG[getApiLang()] ?? DOW_BY_LANG.ko)[day];

export const formatDateWithDow = (dateStr: string) => {
  const date = new Date(dateStr);
  if (Number.isNaN(date.getTime())) return dateStr;
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}.${m}.${d} (${dowLabel(date.getDay())})`;
};

export const addDays = (dateStr: string, days: number) => {
  if (!dateStr) return "";
  const date = new Date(dateStr);
  date.setDate(date.getDate() + days);
  return date.toISOString().slice(0, 10);
};

export const formatMonthDay = (dateStr: string) => {
  const date = new Date(dateStr);
  if (Number.isNaN(date.getTime())) return dateStr;
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${m}.${d}`;
};

export const formatMonthDayWithDow = (dateStr: string) => {
  const date = new Date(dateStr);
  if (Number.isNaN(date.getTime())) return dateStr;
  return `${formatMonthDay(dateStr)} (${dowLabel(date.getDay())})`;
};
