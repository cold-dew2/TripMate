import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";

import translationEN from "./locales/en/translation.json";
import translationKO from "./locales/ko/translation.json";
import translationJA from "./locales/ja/translation.json";

const resources = {
  en: {
    translation: translationEN,
  },
  ko: {
    translation: translationKO,
  },
  ja: { translation: translationJA },
} as const;

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    fallbackLng: "ko",
    interpolation: {
      escapeValue: false,
    },
  });

// <html lang>이 index.html에 "en"으로 고정돼 있으면, 실제로는 한국어/일본어 화면인데도
// 스크린리더가 계속 영어 발음 규칙으로 읽어 내용을 알아듣기 어렵게 만든다(WCAG 3.1.1).
// 사용자가 언어를 바꿀 때마다 실제 표시 언어와 맞춰준다.
const syncHtmlLang = (lng: string) => {
  document.documentElement.lang = lng;
};
syncHtmlLang(i18n.resolvedLanguage ?? i18n.language ?? "ko");
i18n.on("languageChanged", syncHtmlLang);

export default i18n;
