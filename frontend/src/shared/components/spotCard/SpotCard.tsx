import { useState } from "react";
import { useTranslation } from "react-i18next";
import Card from "../card/Card"
import "./SpotCard.css"
import Skeleton from "../skeleton/Skeleton";
import ImagePreparing from "../imagePreparing/ImagePreparing";

interface Props {
  loading?: boolean;
  badge?: string;
  imageUrl?: string;
  title?: string;
  place?: string;
  rating?: string | number;
}

const SpotCard = ({ loading = false, badge, imageUrl, title, place, rating }: Props) => {
  const { t } = useTranslation();
  const [imageBroken, setImageBroken] = useState(false);
  return (
    <Card className="spot-card">
      {loading ? (
        <>
          <div className="img">
            <Skeleton width="100%" height="120px" borderRadius="8px" />
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
            {imageUrl && !imageBroken ? (
              <img
                src={imageUrl}
                alt={t("image.alt", { title })}
                onError={() => setImageBroken(true)}
              />
            ) : (
              <ImagePreparing />
            )}
          </div>
          <div className="info">
            <div className="info-top">

              <p className="title">{title}</p>
              {place && (
                <span className="place">{place}</span>
              )}
            </div>
            {rating && (
              <p className="rating">★ {rating}</p>
            )}
          </div>
        </>
      )}
    </Card>
  )
}

export default SpotCard