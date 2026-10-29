import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { apiClient } from "@/shared/api/client";
import Input from "@/shared/components/input/Input";
import Checkbox from "@/shared/components/checkbox/Checkbox";
import Button from "@/shared/components/button/Button";
import NoticeBox from "@/shared/components/noticeBox/NoticeBox";
import "./WithdrawPage.css";

// 회원 탈퇴: 안내 확인 + 비밀번호 재입력 후 탈퇴한다.
// 다른 멤버가 있는 진행 중 모임을 운영 중이면 서버가 막고 모임 제목을 내려주므로 그 목록을 보여준다.
const WithdrawPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [password, setPassword] = useState("");
  const [agreed, setAgreed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [blockingMoims, setBlockingMoims] = useState<string[]>([]);
  const [done, setDone] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!agreed || !password || submitting) return;
    setSubmitting(true);
    setError("");
    setBlockingMoims([]);
    const result = await apiClient.post("/login/withdraw", { userPw: password });
    setSubmitting(false);

    if (result.success) {
      // 탈퇴 후 로그인 상태의 캐시(프로필 등)가 남아 보이지 않게 전부 비운다.
      queryClient.clear();
      setDone(true);
      return;
    }
    if (result.code === "WRONG_PASSWORD") {
      setError(t("withdraw.wrongPassword"));
    } else if (result.code === "OWNS_ACTIVE_MOIM") {
      const body = result.body as { blockingMoims?: string[] } | undefined;
      setBlockingMoims(body?.blockingMoims ?? []);
      setError(t("withdraw.ownsActiveMoim"));
    } else if (result.code === "NEED_LOGIN") {
      navigate("/auth", { replace: true });
    } else {
      setError(t("withdraw.failed"));
    }
  };

  if (done) {
    return (
      <main className="withdraw-page withdraw-done">
        <h2>{t("withdraw.doneTitle")}</h2>
        <p>{t("withdraw.doneDesc")}</p>
        <Button text={t("withdraw.goHome")} onClick={() => navigate("/", { replace: true })} />
      </main>
    );
  }

  return (
    <main className="withdraw-page">
      <h2 className="withdraw-title">{t("withdraw.heading")}</h2>
      <ul className="withdraw-notice">
        <li>{t("withdraw.notice1")}</li>
        <li>{t("withdraw.notice2")}</li>
        <li>{t("withdraw.notice3")}</li>
        <li>{t("withdraw.notice4")}</li>
      </ul>

      <form className="withdraw-form" onSubmit={submit}>
        <Input
          type="password"
          name="userPw"
          label={t("withdraw.password")}
          placeholder={t("withdraw.passwordPlaceholder")}
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <div className="withdraw-agree">
          <Checkbox
            id="withdraw-agree"
            label={t("withdraw.agree")}
            checked={agreed}
            onChange={(e) => setAgreed(e.target.checked)}
          />
        </div>

        {error && (
          <div className="withdraw-error" role="alert">
            <NoticeBox type="red" desc={error} />
            {blockingMoims.length > 0 && (
              <>
                <ul className="withdraw-blocking">
                  {blockingMoims.map((title) => <li key={title}>{title}</li>)}
                </ul>
                <Link to="/moimManage" className="withdraw-manage">{t("withdraw.manageMoims")}</Link>
              </>
            )}
          </div>
        )}

        <Button
          type="submit"
          variant="destructive"
          size="lg"
          text={submitting ? t("withdraw.submitting") : t("withdraw.submit")}
          disabled={!agreed || !password || submitting}
        />
      </form>
    </main>
  );
};

export default WithdrawPage;
