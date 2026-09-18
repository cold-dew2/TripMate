import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import useMyMoim from "@/features/moim/hooks/useMyMoim";
import Button from "@/shared/components/button/Button";
import { formatDateWithDow } from "@/shared/utils/date";
import "./MySchedule.css";

type ScheduleStatus = "done" | "pending" | "dday";

const getStatus = (moimEndDt: string, stateCd: string): ScheduleStatus => {
  const end = new Date(moimEndDt);
  if (end.getTime() < Date.now()) return "done";
  // 승인 상태 코드가 명확히 확정되기 전까지는 'Y'만 확정으로 간주한다.
  return stateCd === "Y" ? "dday" : "pending";
};

const getDDay = (moimStartDt: string) => {
  const diff = Math.ceil((new Date(moimStartDt).getTime() - Date.now()) / 86400000);
  return diff <= 0 ? "D-day" : `D-${diff}`;
};

const MySchedule = () => {
  const { t } = useTranslation();
  const myMoim = useMyMoim();
  const items = myMoim.data ?? [];

  return (
    <>
      <ul className="schedule-list">
        {myMoim.isLoading ? (
          <li className="schedule-empty">{t("account.loading")}</li>
        ) : myMoim.isError ? (
          <li className="schedule-empty">{t("common.loadError")}</li>
        ) : items.length === 0 ? (
          <li className="schedule-empty">{t("moim.emptyMsg")}</li>
        ) : (
          items.map((moim) => {
            const status = getStatus(moim.moimEndDt, moim.stateCd);
            return (
              <li key={moim.moimId} className="schedule-card">
                <Link to={`/moim/${moim.moimId}`} className="schedule-card-row">
                  <div className="schedule-thumb" aria-hidden="true" />
                  <div className="schedule-info">
                    <p className="schedule-title">{t(moim.moimTitle)}</p>
                    <p className="schedule-meta">{formatDateWithDow(moim.moimStartDt)} · {moim.memberCnt}/{moim.maxMember}{t("명")}</p>
                  </div>
                  <span className={`schedule-badge schedule-badge-${status}`}>
                    {status === "done" ? t("my.statusDone") : status === "pending" ? t("my.statusPending") : getDDay(moim.moimStartDt)}
                  </span>
                </Link>
                {status === "done" && (
                  <Link to={`/moim/${moim.moimId}/review`} className="schedule-review-btn">
                    {t("my.registerReviewBtn")}
                  </Link>
                )}
              </li>
            );
          })
        )}
      </ul>

      <Button as={Link} to="/moimManage" text={t("my.myMoim")} />
    </>
  );
};

export default MySchedule;
