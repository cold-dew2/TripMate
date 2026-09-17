import i18n from "@/i18n";

// 백엔드 API가 이해하는 언어 코드(ko/en/ja)로 정규화한다.
export const getApiLang = () => {
  const lng = i18n.language ?? "ko";
  if (lng.startsWith("ja")) return "ja";
  if (lng.startsWith("en")) return "en";
  return "ko";
};
