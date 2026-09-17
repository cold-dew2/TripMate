import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import i18n from "@/i18n";
import { getApiLang } from "@/shared/utils/lang";
import "./LanguageSwitcher.css";

const LANGUAGES = [
  { value: "ko", label: "한국어" },
  { value: "en", label: "English" },
  { value: "ja", label: "日本語" },
] as const;

const LanguageSwitcher = () => {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const wrapRef = useRef<HTMLDivElement>(null);
  const current = getApiLang();

  useEffect(() => {
    if (!isOpen) return;

    const handleOutside = (event: MouseEvent) => {
      if (wrapRef.current && !wrapRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setIsOpen(false);
    };
    document.addEventListener("mousedown", handleOutside);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handleOutside);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen]);

  const select = (value: string) => {
    i18n.changeLanguage(value);
    setIsOpen(false);
  };

  const currentLabel = LANGUAGES.find((lang) => lang.value === current)?.label ?? LANGUAGES[0].label;

  return (
    <div className="language-switcher" ref={wrapRef}>
      <button
        type="button"
        className="language-switcher-trigger"
        aria-haspopup="true"
        aria-expanded={isOpen}
        aria-label={t("home.language")}
        onClick={() => setIsOpen((prev) => !prev)}
      >
        <span>{currentLabel}</span>
        <span className="language-switcher-caret" aria-hidden="true" />
      </button>
      {isOpen && (
        <ul className="language-switcher-list" aria-label={t("home.language")}>
          {LANGUAGES.map((lang) => (
            <li key={lang.value}>
              <button
                type="button"
                className={lang.value === current ? "is-active" : ""}
                aria-current={lang.value === current}
                onClick={() => select(lang.value)}
              >
                {lang.label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default LanguageSwitcher;
