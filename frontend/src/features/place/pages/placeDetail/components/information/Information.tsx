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
        // AI가 잠시 응답하지 못하는 경우(외부 AI 서비스 과부하 등)까지 "실패했다"는
        // 경고성 문구를 보여주면 사용자가 앱이 고장난 것처럼 느낀다. 아직 준비 중인
        // 콘텐츠라는 부담 없는 톤으로 대신 안내한다.
        <PageState
          status={isAiUnavailable ? 'empty' : 'error'}
          message={isAiUnavailable ? t('place.infoComingSoon') : t('place.infoError')}
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
