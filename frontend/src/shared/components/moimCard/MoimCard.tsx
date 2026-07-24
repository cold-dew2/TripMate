import React from 'react'
import Card from '../card/Card'
import Skeleton from '../skeleton/Skeleton'
import { useTranslation } from 'react-i18next';
import "./MoimCard.css"

interface Props {
  loading?: boolean;
  badge?: string;
  imageUrl?: string;
  title?: string;
  place?: string;
  date?: string;
  member?: string;
  views?: number;
}

const MoimCard = ({ loading = false, badge, imageUrl, title, date, member, views }: Props) => {
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
              {badge.split(",").map((item) => (
                <span key={item} className="badge">
                  #{item}
                </span>
              ))}
            </div>
          )}
          <div className="img">
            <img src={imageUrl} alt={t("image.alt", { title })} />
            <p className="title">{title}</p>
          </div>
          <div className="info">
            <div className="info-left">
              {date && (
                <span className="place">🗓️ {date}</span>
              )}
              {member && (
                  <span className="member">👥 {t("people", { count: member })}</span>
              )}
            </div>
            {views !== undefined && (
                <div className="views">🔥 {views ?? 0}</div>
            )}
          </div>
        </>
      )}
    </Card>
  )
}

export default MoimCard