import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import useUser from "@/shared/hooks/useUser";
import useUnreadNotificationCount from "@/shared/hooks/useUnreadNotificationCount";
import Button from "@/shared/components/button/Button";
import Input from "@/shared/components/input/Input";
import LanguageSwitcher from "@/shared/components/languageSwitcher/LanguageSwitcher";
import "./HomeHeader.css"

const HomeHeader = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const { data: user, isError, error } = useUser();
  const { data: unreadCount = 0 } = useUnreadNotificationCount();

  const needsLogin = (error as { code?: string } | undefined)?.code === "NEED_LOGIN";

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
            {isError && !needsLogin ? (
              <span>{t("home.userError")}</span>
            ) : user ? (
                t("home.userName", { name: user.userNm })
            ) : (
              t("home.guest")
            )}
          </p>
        </div>
        <div className="buttons">
          <LanguageSwitcher />
          {user && (
            <span className="notice-btn-wrap">
              <Button
                as={Link}
                to="/notifications"
                variant="ghost"
                size="icon"
                icon
                text={t("home.notice")}
                img="/icons/icon-notice.png"
              />
              {unreadCount > 0 && (
                <span className="notice-badge">{unreadCount > 99 ? "99+" : unreadCount}</span>
              )}
            </span>
          )}
          <Button
            as={Link}
            to="/my"
            variant="ghost"
            size="icon"
            icon
            text={t("home.mypage")}
            img="/icons/icon-my.png"
          />
        </div>
      </div>
      <div className="header-search">
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
