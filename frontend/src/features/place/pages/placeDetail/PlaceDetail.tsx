import { useParams } from 'react-router-dom'
import usePlaceDetail from '../../hooks/usePlaceDetail';
import { useTranslation } from 'react-i18next';
import './PlaceDetail.css'
import Tab from '@/shared/components/tab/Tab';

const tabs = [
  { id: "tab1", label: "소개" },
  { id: "tab2", label: "정보" },
  { id: "tab3", label: "후기" },
];

const PlaceDetail = () => {
  const { t } = useTranslation();
  const { tourId } = useParams<{ tourId: string }>();
  const { data: place, isLoading, isError } = usePlaceDetail(tourId ?? "");

  if (isLoading) return <div>{tourId}로딩</div>;
  if (isError || !place) return <div>{tourId}nodata</div>;

  const infos = [
    { icons: "icon_time.svg", title: t("place.opentime"), value: place.operatingHours },
    { icons: "icon_dayOff.svg", title: t("place.dayOff"), value: place.closedDays },
    { icons: "icon_star2.svg", title: t("place.fee"), value: place.admissionFeeIsFree === "Y" ? "무료" : "유료", desc: place.admissionFeeDetails },
    { icons: "icon_parking.svg", title: t("place.parking"), value: place.parkingAvailable === "Y" ? "가능" : "불가능", desc: place.parkingFeeInfo },
    { icons: "icon_global.svg", title: t("place.url"), value: place.websiteUrl },
  ]

  return (
    <div className="place-detail">
      <div className="detail-header">
        <div className="detail-img">
          {/* <img src={`place.firstImage`} alt={`${place.tourNm}의 이미지`} /> */}
          <img src={`/images/places/${place.tourId}.jpeg`} alt="" />
        </div>
        <div className="detail-info">
          <div className="place-name">{t(place.tourNm)}</div>
          <div className="place-addr">{t(place.roadAddr)}</div>
          <div className="place-avgScore">
            <img src="/icons/icon_star.png" alt="" />
            <span>{place.avgScore}</span>
          </div>
        </div>
      </div>

      <div className="detail-content">
        <Tab tabs={tabs} type="scroll" />

        <div className="tab-contents">
          <div className="tab-content" id="tab1" tabIndex={-1}>
            <span className="place-info">관광지 상세 영역</span>
            <div>
              지도영역
            </div>
          </div>
          <div className="tab-content" id="tab2" tabIndex={-1}>
            <div className="tab-title">
              <p>{t("place.review")}</p>
              <span><button type="button">{t("place.info")}</button></span>
            </div>

            <ul className="place-infoList">
              {infos.map((info, index) => (
                <li key={index}>
                  <span className="icon"><img src={`/icons/${info.icons}`} /></span>
                  <div className="info-wrap">
                    <span className="info-title">{info.title}</span>
                    <p className="info-desc">{info.value}</p>
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
          <div className="tab-content" id="tab3" tabIndex={-1}>
            <div className="tab-title">
              <p>{t("place.review")}</p>
              <span><button type="button">{t("place.reviewsBtn")}</button></span>
            </div>

            <div className="reviews-content">
              <div className="reviews">

              </div>
            </div>

            <ul>
              <li></li>
            </ul>
          </div>

        </div>
      </div>
    </div>
  );
};

export default PlaceDetail; 