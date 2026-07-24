import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import i18n from "@/i18n";
import Button from "@/shared/components/button/Button";
import "./HomeHeader.css"
import useUser from "@/shared/hooks/useUser";
import Input from "@/shared/components/input/Input";

const HomeHeader = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const { data: user, isError, error } = useUser();

  const status = (error as Error & { status?: number } | undefined)?.status;

  const handleSearchSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) return;
    navigate(`/search?q=${encodeURIComponent(trimmed)}`);
  }
  return (
    <header className="header">
      <div className="header-top">
        <div className="user">
          <span className="greeting">
            {t("home.greeting")}
          </span>
          <p className="name">
            {isError && status !== 401 ? (
              <span>{t("home.userError")}</span>
            ) : user ? (
                t("home.userName", { name: user.userNm })
            ) : (
              t("home.guest")
            )}
          </p>
        </div>
        <div className="buttons">
          <button onClick={() => i18n.changeLanguage("en")}>
            EN
          </button>
          <button onClick={() => i18n.changeLanguage("ko")}>
            ko
          </button>
          <Button
            as={Link}
            to="/notice"
            variant="ghost"
            size="icon"
            icon
            text={t("home.notice")}
            img="/icons/icon-notice.png"
          />
          <Button
            as={Link}
            to="/mypage"
            variant="ghost"
            size="icon"
            icon
            text={t("home.mypage")}
            img="/icons/icon-my.png"
          />
        </div>
      </div>
      <div className="header-search">
        <label htmlFor="home-search" className="blind">
          {t("home.searchLabel")}
        </label>
        <form onSubmit={handleSearchSubmit}>
          <Input
            label={t("home.searchLabel")}
            id="home-search"
            name="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={t("home.searchPlaceholder")}
            blind
          />
        </form>
      </div>
    </header>
  )
}

export default HomeHeader