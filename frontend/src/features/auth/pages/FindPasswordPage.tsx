import { useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { apiClient } from "@/shared/api/client";
import Input from "@/shared/components/input/Input";
import Button from "@/shared/components/button/Button";
import "./AuthPage.css";

interface VerifyFormValues {
  userId: string;
  userNm: string;
}

interface NewPwFormValues {
  newUserPw: string;
  newUserPwConfirm: string;
}

const FindPasswordPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [verified, setVerified] = useState<VerifyFormValues | null>(null);
  const [verifyError, setVerifyError] = useState("");
  const [resetError, setResetError] = useState("");
  const [resetSuccess, setResetSuccess] = useState(false);

  const verifyForm = useForm<VerifyFormValues>({ defaultValues: { userId: "", userNm: "" } });
  const newPwForm = useForm<NewPwFormValues>({ defaultValues: { newUserPw: "", newUserPwConfirm: "" } });

  const onVerify = async (values: VerifyFormValues) => {
    setVerifyError("");
    const result = await apiClient.post<unknown>("/login/verifyReset", values);

    if (!result.success) {
      setVerifyError(t("account.findPwNotFound"));
      return;
    }

    setVerified(values);
  };

  const onReset = async (values: NewPwFormValues) => {
    if (!verified) return;
    setResetError("");

    if (values.newUserPw !== values.newUserPwConfirm) {
      setResetError(t("account.passwordMismatch"));
      return;
    }

    const result = await apiClient.post<unknown>("/login/resetPassword", {
      userId: verified.userId,
      userNm: verified.userNm,
      newUserPw: values.newUserPw,
    });

    if (!result.success) {
      setResetError(t("account.resetPwFailed"));
      return;
    }

    setResetSuccess(true);
  };

  if (resetSuccess) {
    return (
      <main className="auth-screen">
        <h1 className="auth-title">{t("account.resetPwSuccess")}</h1>
        <div className="auth-form">
          <Button type="button" text={t("account.backToLogin")} onClick={() => navigate("/auth")} />
        </div>
      </main>
    );
  }

  if (verified) {
    return (
      <main className="auth-screen">
        <header className="auth-header">
          <button type="button" className="auth-back" onClick={() => setVerified(null)} aria-label={t("account.back")}>‹</button>
          <h1>{t("account.findPw")}</h1>
        </header>

        <form className="auth-form" onSubmit={newPwForm.handleSubmit(onReset)} noValidate>
          <Input
            label={t("account.newPassword")}
            id="newUserPw"
            type="password"
            placeholder={t("account.passwordPlaceholder")}
            error={newPwForm.formState.errors.newUserPw?.message}
            {...newPwForm.register("newUserPw", {
              required: t("account.passwordRequired"),
              minLength: { value: 8, message: t("account.passwordMin") },
            })}
          />
          <Input
            label={t("account.newPasswordConfirm")}
            id="newUserPwConfirm"
            type="password"
            placeholder={t("account.passwordPlaceholder")}
            error={newPwForm.formState.errors.newUserPwConfirm?.message}
            {...newPwForm.register("newUserPwConfirm", { required: t("account.passwordRequired") })}
          />

          {resetError && <p className="auth-error">{resetError}</p>}

          <Button
            type="submit"
            text={newPwForm.formState.isSubmitting ? t("common.saving") : t("account.resetPwSubmit")}
            disabled={newPwForm.formState.isSubmitting}
          />
        </form>
      </main>
    );
  }

  return (
    <main className="auth-screen">
      <header className="auth-header">
        <button type="button" className="auth-back" onClick={() => navigate(-1)} aria-label={t("account.back")}>‹</button>
        <h1>{t("account.findPw")}</h1>
      </header>

      <form className="auth-form" onSubmit={verifyForm.handleSubmit(onVerify)} noValidate>
        <Input
          label={t("account.email")}
          id="userId"
          placeholder={t("account.emailPlaceholder")}
          error={verifyForm.formState.errors.userId?.message}
          {...verifyForm.register("userId", { required: t("account.emailRequired") })}
        />
        <Input
          label={t("account.name")}
          id="userNm"
          placeholder={t("account.namePlaceholder")}
          error={verifyForm.formState.errors.userNm?.message}
          {...verifyForm.register("userNm", { required: t("account.nameRequired") })}
        />

        {verifyError && <p className="auth-error">{verifyError}</p>}

        <Button
          type="submit"
          text={verifyForm.formState.isSubmitting ? t("common.saving") : t("account.next")}
          disabled={verifyForm.formState.isSubmitting}
        />
      </form>

      <div className="auth-links">
        <Link to="/auth">{t("account.backToLogin")}</Link>
      </div>
    </main>
  );
};

export default FindPasswordPage;
