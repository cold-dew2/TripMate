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

      {place && (() => {
        // 한국관광공사 TourAPI가 준 값만 그대로 보여주고, 없는 항목은 줄 자체를 숨긴다.
        // (예전엔 AI 응답의 "true"/"false"를 "Y"와 비교해 항상 "유료/이용 불가"로 잘못 표시됐다.)
        const parkingDesc = place.parkingFeeInfo || undefined;
        const contact = [place.infoCenter, place.tel].filter(Boolean)
          .filter((v, i, arr) => arr.indexOf(v) === i).join('\n');
        // 항목별 출처: TourAPI 공식 값과 AI가 출처를 확인해 보완한 값을 구분해서 보여준다.
        const researchOf = (keys: string[]) =>
          keys.map((k) => place.fields?.[k]).find((f) => f?.origin === 'AI_RESEARCH');
        const rows = [
          { icons: "icon_time.svg", title: t("place.opentime"), value: place.operatingHours, keys: ['operatingHours'] },
          { icons: "icon_dayOff.svg", title: t("place.dayOff"), value: place.closedDays, keys: ['closedDays'] },
          { icons: "icon_star2.svg", title: t("place.fee"), value: place.admissionFeeDetails, keys: ['admissionFeeDetails'] },
          { icons: "icon_parking.svg", title: t("place.parking"), value: place.parkingInfo, desc: parkingDesc, keys: ['parkingInfo', 'parkingFeeInfo'] },
          { icons: "icon_info.svg", title: t("place.contact"), value: contact, keys: ['infoCenter', 'tel'] },
          { icons: "icon_global.svg", title: t("place.url"), link: place.websiteUrl, keys: ['websiteUrl'] },
        ].filter((info) => info.value || info.desc || info.link);

        if (rows.length === 0 || place.source === 'NONE') {
          return (
            <PageState
              status={place.researchPending ? 'loading' : 'empty'}
              message={place.researchPending ? t('place.researching') : t('place.infoEmpty')}
              fullScreen={false}
            />
          );
        }

        const sourceNote = place.source === 'TOURAPI_TRANSLATED' ? t('place.infoSourceTranslated')
          : place.source === 'TOURAPI_KO' ? t('place.infoSourceKo')
          : t('place.infoSource');

        return (
          <>
            <ul className="place-infoList">
              {rows.map((info, index) => (
                <li key={index}>
                  <span className="icon"><img src={`/icons/${info.icons}`} alt="" /></span>
                  <div className="info-wrap">
                    <span className="info-title">{info.title}</span>
                    {info.value && <p className="info-desc" style={{ whiteSpace: 'pre-line' }}>{info.value}</p>}
                    {info.link && (
                      <p className="info-desc">
                        <Link to={info.link} target="_blank" rel="noopener noreferrer" className="info-link">
                          {info.link}
                        </Link>
                      </p>
                    )}
                    {info.desc && (
                      <span className="desc-info" style={{ whiteSpace: 'pre-line' }}>{info.desc}</span>
                    )}
                    {(() => {
                      const research = researchOf(info.keys);
                      if (!research) return null;
                      return (
                        <span className="desc-info ai-research-note">
                          {t('place.aiResearched')}
                          {research.sourceUrl && (
                            <>
                              {' '}
                              <a href={research.sourceUrl} target="_blank" rel="noopener noreferrer">
                                {t('place.aiResearchSource')}: {research.sourceTitle || research.sourceUrl}
                              </a>
                            </>
                          )}
                          {research.checkedAt && <> · {t('place.checkedAt')} {research.checkedAt.slice(0, 10)}</>}
                        </span>
                      );
                    })()}
                  </div>
                </li>
              ))}
            </ul>

            {place.researchPending && <p className="notification">{t('place.researching')}</p>}
            <p className="notification">{t('place.lastUpdatedNote')}<br />{sourceNote}</p>
          </>
        );
      })()}
    </div>
  )
}

export default Information
