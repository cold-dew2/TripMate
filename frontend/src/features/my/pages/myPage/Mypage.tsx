import Button from "@/shared/components/button/Button";
import "./Mypage.css"
import { useTranslation } from 'react-i18next';
import Navigation from "@/layouts/components/nav/Navigation";

const Mypage = () => {
  const { t } = useTranslation();


  return (
    <>
      <div className="my-container">
        <div className="user-profile">
          <div className="user photo">
            <input type="file" name="" id="" />
          </div>
          <Button text="프로필 편집" variant="secondary" size="sm" className="btn-editor"/>
        </div>
        <div className="user-info">
          <p className="user-name">{t("my.userName")}</p>
          <div className="user-rating">
            div.star
          </div>
          <p className="user-desc">{t("my.userDesc")}</p>
        </div>
      </div>

      <Navigation />
    </>
  )
}

export default Mypage