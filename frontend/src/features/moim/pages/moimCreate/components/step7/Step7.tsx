import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import Button from "@/shared/components/button/Button";
import { useAlert } from "@/shared/contexts/AlertContext";
import "./Step7.css";

interface Step7Props {
  moimId: string | null;
}

const Step7 = ({ moimId }: Step7Props) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { showAlert } = useAlert();

  const handleShare = async () => {
    const url = moimId ? `${window.location.origin}/moim/${moimId}` : window.location.origin;
    if (navigator.share) {
      try {
        await navigator.share({ title: t("moimCreate.title"), url });
        return;
      } catch {
        // 공유 취소 시 클립보드 복사로 폴백
      }
    }
    await navigator.clipboard.writeText(url);
    showAlert(t("moimCreate.step7.shareCopied"));
  };

  return (
    <div className="create-content step7-content">
      <div className="step7-check" aria-hidden="true">✓</div>
      <h2 className="step7-title">{t("moimCreate.step7.title")}</h2>
      <p className="step7-subtitle">{t("moimCreate.step7.subtitle")}</p>

      <div className="step7-buttons">
        <Button
          text={t("moimCreate.step7.viewDetail")}
          onClick={() => moimId && navigate(`/moim/${moimId}`)}
          disabled={!moimId}
        />
        <Button text={t("moimCreate.step7.share")} variant="secondary" onClick={handleShare} />
      </div>
    </div>
  );
};

export default Step7;
