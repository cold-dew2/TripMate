import "./Mypage.css"
import { useTranslation } from 'react-i18next';

const Mypage = () => {
  const { t } = useTranslation();


  return (
    <div className="my-container">
      <div className="user-info">
        <p className="user-name">{t("my.userName")}</p>
        <div className="user-rating">
          div.star
        </div>
        <p className="user-desc">{t("my.userDesc")}</p>
      </div>

    </div>
  )
}

export default Mypage