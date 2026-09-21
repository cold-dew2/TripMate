import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { usePlaceAIDetail } from '@/features/place/hooks/usePlaceDetail';
import PageState from '@/shared/components/pageState/PageState';
import useAiWaitNotice from '@/shared/hooks/useAiWaitNotice';
import './Information.css'

interface InformationProps {
  tourId: string;
}

const Information = ({ tourId }: InformationProps) => {
  const { t } = useTranslation();
  const { data: place, isLoading, isError, error } = usePlaceAIDetail(tourId);
  const isAiUnavailable = (error as { code?: string } | null)?.code === 'AI_UNAVAILABLE';
  useAiWaitNotice(isLoading, t('place.infoLoading'));

  return (
    <div className="info-content">
      <div className="title-wrap">
        <p>{t("place.info")}</p>
      </div>

      {isLoading && <PageState status="loading" message={t("place.infoLoading")} fullScreen={false} />}
      {isError && (
        <PageState
          status="error"
          message={isAiUnavailable ? t('common.aiUnavailable') : t('place.infoError')}
          fullScreen={false}
        />
      )}

      {place && (
        <>
          <ul className="place-infoList">
            {[
              { icons: "icon_time.svg", title: t("place.opentime"), value: place.operatingHours },
              { icons: "icon_dayOff.svg", title: t("place.dayOff"), value: place.closedDays },
              { icons: "icon_star2.svg", title: t("place.fee"), value: place.admissionFeeIsFree === "Y" ? t("place.free") : t("place.paid"), desc: place.admissionFeeDetails },
              { icons: "icon_parking.svg", title: t("place.parking"), value: place.parkingAvailable === "Y" ? t("place.available") : t("place.unavailable"), desc: place.parkingFeeInfo },
              { icons: "icon_global.svg", title: t("place.url"), link: place.websiteUrl },
            ].map((info, index) => (
              <li key={index}>
                <span className="icon"><img src={`/icons/${info.icons}`} alt="" /></span>
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
        </>
      )}
    </div>
  )
}

export default Information
