import Button from '@/shared/components/button/Button'
import Input from '@/shared/components/input/Input';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router-dom'

const GuidePage = () => {

  const { t } = useTranslation();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");

  const handleSearchSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) return;
    navigate(`/search?q=${encodeURIComponent(trimmed)}`)
  }
  
  return (
    <>
      <div>button</div>

      <div>

        <div className="buttons" style={{width: '100px', height: '40px'}}>
            <Button as={Link} to="/" variant="ghost" icon size="icon" text={t("home.notice")} img="/icons/icon-notice.png" />
            <Button as={Link} to="/mypage" size="icon" icon text={t("home.mypage")} img="/icons/icon-my.png" />
        </div>

        <Button text="Primary Button" />
        <Button text="Primary Button disabled" disabled />
        <Button text="secondary Button" variant="secondary" />
        <Button text="secondary Button disabled" variant="secondary" disabled />
        <Button text="ghost Button" variant="ghost" />
        <Button text="ghost Button disabled" variant="ghost" disabled />
        <Button text="destructive Button" variant="destructive" />
        <Button text="destructive Button disabled" variant="destructive" disabled />

        <div className="buttons">
          <Button text="secondary Button" variant="secondary" />
          <Button text="Primary Button" />
        </div>

        <div className="buttons flex-2">
          <Button text="secondary Button" variant="secondary" />
          <Button text="Primary Button" />
        </div>
      </div>

      <div>input</div>
      <div className="input">


        <form onSubmit={handleSearchSubmit}>
          <Input className="header-search" label={t("home.searchLabel")} placeholder={t("home.searchPlaceholder")} name="search" id="home-search" blind onChange={(e) => setQuery(e.target.value)} />
        </form>

        <Input label={t("home.searchLabel")} placeholder={t("home.searchPlaceholder")} name="search" id="home-search" blind />
        <Input label={t("home.searchLabel")} placeholder={t("home.searchPlaceholder")} name="search" id="home-search" blind disabled />
        <Input label={t("home.searchLabel")} placeholder={t("home.searchPlaceholder")} name="search" id="home-search" blind readonly />
      </div>
    </>
  
  )
}

export default GuidePage