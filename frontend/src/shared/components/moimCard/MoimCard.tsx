import Card from '../card/Card'
import Skeleton from '../skeleton/Skeleton'
import { useTranslation } from 'react-i18next';
import "./MoimCard.css"

interface Props {
  loading?: boolean;
  badge?: string;
  imageUrl?: string;
  title?: string;
  desc?: string;
  place?: string;
  date?: string;
  member?: string;
  maxMember?: string;
  views?: number;
  userNm?: string;
  userRating?: string;
}

const MoimCard = ({ loading = false, badge, imageUrl, title, date, member, maxMember, views, desc, place, userNm, userRating }: Props) => {
  const { t } = useTranslation();
  return (
    <Card className="moim-card">
      {loading ? (
        <>
          <div className="img">
            <Skeleton width="100%" height="160px" borderRadius="8px" />
          </div> 

          <div className="info">
            <div className="info-top">
              <Skeleton width="70%" height="18px" />
              <Skeleton width="45%" height="14px" />
            </div>

            <Skeleton width="30%" height="14px" />
          </div>
        </>
      ) : (
        <>
          {badge && (
            <div className="badges">
              {badge.split(",").slice(0, 5).map((item) => (
                <span key={item} className="badge">
                  #{item}
                </span>
              ))}
            </div>
          )}
          <div className="img">
            <img
              src={imageUrl || "/images/places/no-image.svg"}
              alt={t("image.alt", { title })}
              onError={(event) => { event.currentTarget.src = "/images/places/no-image.svg"; }}
            />
            <p className="title">{title}</p>
          </div>
          {desc && (
            <p className="desc">{desc}</p>
          )}
          <div className={`info ${views ? "views" : ""}`}>
            <div className="info-left">
              {(date || place) && (
                  <div className="date-place">
                    <span className="date">{date ? `🗓️ ${date} ` : undefined}</span>
                    <span className="place">{place ? `📍${place}` : undefined}</span>
                  </div>
              )}
              {member !== undefined && (
                  <span className="member">
                    👥 {t("people", { count: member })}
                                {maxMember ? ` / ${maxMember}` : ""}
                  </span>
              )}
            </div>
            {views !== undefined && (
                <div className="views">🔥 {views ?? 0}</div>
            )}
          </div>
          {userNm && (
            <div className="user-info">
                <span className="user-name">{userNm}</span>
                <span className="user-rating">★ {userRating}</span>
            </div>
          )}
        </>
      )}
    </Card>
  )
}

export default MoimCard