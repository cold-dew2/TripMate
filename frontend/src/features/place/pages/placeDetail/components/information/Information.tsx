import type { PlaceAIDetail } from '@/types/place';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import './Information.css'

interface InformationProps { 
  place: PlaceAIDetail; 
}

const Information = ({ place }: InformationProps) => {
  const { t } = useTranslation();

  const infos = [
    { icons: "icon_time.svg", title: t("place.opentime"), value: place.operatingHours },
    { icons: "icon_dayOff.svg", title: t("place.dayOff"), value: place.closedDays },
    { icons: "icon_star2.svg", title: t("place.fee"), value: place.admissionFeeIsFree === "Y" ? "무료" : "유료", desc: place.admissionFeeDetails },
    { icons: "icon_parking.svg", title: t("place.parking"), value: place.parkingAvailable === "Y" ? "가능" : "불가능", desc: place.parkingFeeInfo },
    { icons: "icon_global.svg", title: t("place.url"), link: place.websiteUrl },
  ]

  return (
    <div className="info-content">
      <div className="tab-title">
        <p>{t("place.info")}</p>
      </div>

      <ul className="place-infoList">
        {infos.map((info, index) => (
          <li key={index}>
            <span className="icon"><img src={`/icons/${info.icons}`} /></span>
            <div className="info-wrap">
              <span className="info-title">{info.title}</span>
              <p className="info-desc">{info.value}</p>
              {info.link && (
                <Link to={info.link} target="_blank" rel="noopener noreferrer" className="info-link">
                  {info.link}
                </Link>
              )}
              {info.desc && (
                <span className="desc-info">{info.desc}</span>
              )}
            </div>
          </li>
        ))}
      </ul>

      {place.lastUpdatedNote && (
        <p className="notification">{place.lastUpdatedNote}</p>
      )}
    </div>
  )
}

export default Information