import { useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { apiClient } from "@/shared/api/client";
import Select from "@/shared/components/select/Select";
import Textarea from "@/shared/components/textarea/Textarea";
import Button from "@/shared/components/button/Button";
import useMyMoim from "@/features/moim/hooks/useMyMoim";
import useMoimMembers from "@/features/moim/hooks/useMoimMembers";
import "./SafetyReportScreen.css";

interface FormValues {
  moimId: string;
  targetUserId: string;
  reportType: string;
  content: string;
}

const REPORT_TYPES = [
  { value: "SAFETY", key: "safetyReport.typeSafety" },
  { value: "RUDE", key: "safetyReport.typeRude" },
  { value: "NOSHOW", key: "safetyReport.typeNoshow" },
  { value: "ETC", key: "safetyReport.typeEtc" },
];

const SafetyReportScreen = () => {
  const { t } = useTranslation();
  const [submitted, setSubmitted] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const {
    register,
    watch,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ defaultValues: { moimId: "", targetUserId: "", reportType: "SAFETY", content: "" } });

  const selectedMoimId = watch("moimId");
  const myMoim = useMyMoim();
  const members = useMoimMembers(selectedMoimId);

  const moimOptions = (myMoim.data ?? []).map((moim) => ({ value: moim.moimId, option: moim.moimTitle }));
  const memberOptions = (members.data ?? []).map((member) => ({ value: member.userId, option: member.userNm }));

  const onSubmit = async (values: FormValues) => {
    setSubmitError("");
    const result = await apiClient.post("/safety-reports", {
      moimId: values.moimId,
      targetUserId: values.targetUserId,
      reportType: values.reportType,
      content: values.content,
    });
    if (!result.success) {
      setSubmitError(t("common.loadError"));
      return;
    }
    setSubmitted(true);
  };

  return (
    <main className="safety-report-screen">
      <div className="safety-report-notice">
        <p>{t("safetyReport.notice")}</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <Select
          label={t("safetyReport.moimName")}
          id="moimId"
          placeholder={t("safetyReport.selectMoim")}
          options={moimOptions}
          error={errors.moimId?.message}
          {...register("moimId", { required: t("safetyReport.moimRequired") })}
        />

        <Select
          label={t("safetyReport.personName")}
          id="targetUserId"
          placeholder={selectedMoimId ? t("safetyReport.selectPerson") : t("safetyReport.selectMoimFirst")}
          disabled={!selectedMoimId}
          options={memberOptions}
          error={errors.targetUserId?.message}
          {...register("targetUserId", { required: t("safetyReport.personRequired") })}
        />

        <div className="safety-report-type">
          <span className="safety-report-type-label">{t("safetyReport.typeLabel")}</span>
          <div className="safety-report-type-chips">
            {REPORT_TYPES.map((type) => (
              <label key={type.value} className="safety-report-chip">
                <input type="radio" value={type.value} {...register("reportType")} />
                <span>{t(type.key)}</span>
              </label>
            ))}
          </div>
        </div>

        <Textarea
          label={t("safetyReport.safetydesc")}
          id="content"
          placeholder={t("safetyReport.placeholder")}
          rows={5}
          error={errors.content?.message}
          {...register("content", { required: t("safetyReport.contentRequired") })}
        />

        {submitError && <p className="safety-report-error">{submitError}</p>}
        {submitted && <p className="safety-report-success">{t("safetyReport.success")}</p>}

        <Button
          type="submit"
          variant="negative"
          text={isSubmitting ? t("common.saving") : t("common.declaration")}
          disabled={isSubmitting}
        />
      </form>
    </main>
  );
};

export default SafetyReportScreen;
