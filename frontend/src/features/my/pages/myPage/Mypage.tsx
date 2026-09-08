import Button from "@/shared/components/button/Button";
import "./Mypage.css"
import { useTranslation } from 'react-i18next';
import Navigation from "@/layouts/components/nav/Navigation";
import ReviewsList from "@/shared/components/reviewsList/ReviewsList";
import { Link } from "react-router-dom";

const reviews =[
  {
      "moimTitle": "서울 궁궐 투어",
      "moimStartDt": "2026-08-15",
      "moimEndDt": "2026-08-15",
      "reviewTitle": "정말 즐거운 시간이었어요!",
      "reviewContent": "친구들과 함께 경복궁을 둘러봤는데 생각보다 볼거리가 많아서 재미있었습니다. 모임 분위기도 좋았고 다음에도 참여하고 싶어요.",
      "reviewScore": 5,
      "creatDt": "2026-08-16",
      "userNm": "김민지"
    },
    {
      "moimTitle": "서울 궁궐 투어",
      "moimStartDt": "2026-08-15",
      "moimEndDt": "2026-08-15",
      "reviewTitle": "정말 즐거운 시간이었어요!",
      "reviewContent": "친구들과 함께 경복궁을 둘러봤는데 생각보다 볼거리가 많아서 재미있었습니다. 모임 분위기도 좋았고 다음에도 참여하고 싶어요.",
      "reviewScore": 5,
      "creatDt": "2026-08-16",
      "userNm": "김민지"
    },
]
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
          <span className="user-area">{t("my.userArea")}</span>
          <div className="user-rating">
            div.star
          </div>
          <p className="user-desc">{t("my.userDesc")}</p>
          <ul className="info-list">
            <li>
              <p className="info-num">23</p>
              <p className="info-title">{t("my.moiming")}</p>
            </li>
            <li>
              <p className="info-num">347</p>
              <p className="info-title">{t("my.member")}</p>
            </li>
            <li>
              <p className="info-num">42</p>
              <p className="info-title">{t("my.getReview")}</p>
            </li>
          </ul>
        </div>

        <div className="lang-area">
          <div className="title-wrap">{t("my.useLang")}</div>
        
          <ul className="info-list">
            <li>
              <p className="info-text">{t("lang.ko")}</p>
              <p className="info-title">원어민</p>
            </li>
            <li>
              <p className="info-text">{t("lang.en")}</p>
              <p className="info-title">유창</p>
            </li>
            <li>
              <p className="info-text">{t("lang.jp")}</p>
              <p className="info-title">기초</p>
            </li>
          </ul>
        </div>

        <div className="reviews-area">
          <div className="title-wrap">{t("my.recentlyReviews")}</div>
          <ReviewsList reviews={reviews}/>
        </div>

        <ul className="myPage-list">
          <li><Link to="/">{t("my.myPlan")}</Link></li>
          <li><Link to="/">{t("my.myMoim")}</Link></li>
          <li><Link to="/">{t("my.declaration")}</Link></li>
        </ul>

        <button type="button" className="btn-logout">{t("my.logout")}</button>
      </div>

      <Navigation />
    </>
  )
}

export default Mypage