import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import useMyMoim from "@/features/moim/hooks/useMyMoim";
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
            const isHost = moim.roleCd === "A";
            return (
              <li key={moim.moimId} className="schedule-card">
                <div className="schedule-card-row">
                  <Link to={`/moim/${moim.moimId}`} className="schedule-card-main">
                    <div className="schedule-thumb" aria-hidden="true" />
                    <div className="schedule-info">
                      <p className="schedule-title">{t(moim.moimTitle)}</p>
                      <p className="schedule-meta">{formatDateWithDow(moim.moimStartDt)} · {moim.memberCnt}/{moim.maxMember}{t("명")}</p>
                    </div>
                  </Link>
                  <div className="schedule-card-side">
                    <span className={`schedule-badge schedule-badge-${status}`}>
                      {status === "done" ? t("my.statusDone") : status === "pending" ? t("my.statusPending") : getDDay(moim.moimStartDt)}
                    </span>
                    {isHost && (
                      <Link to={`/moimManage/${moim.moimId}`} className="schedule-manage-btn">
                        {t("moim.manageBtn")}
                      </Link>
                    )}
                  </div>
                </div>
                {status === "done" && (
                  moim.reviewedYn === "Y" ? (
                    <span className="schedule-review-btn schedule-review-done">
                      {t("my.reviewCompleted")}
                    </span>
                  ) : (
                    <Link to={`/moim/${moim.moimId}/review`} className="schedule-review-btn">
                      {t("my.registerReviewBtn")}
                    </Link>
                  )
                )}
              </li>
            );
          })
        )}
      </ul>
    </>
  );
};

export default MySchedule;
