import { useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { apiClient } from "@/shared/api/client";
import Input from "@/shared/components/input/Input";
import Button from "@/shared/components/button/Button";
import "./AuthPage.css";

interface FindIdFormValues {
  userNm: string;
}

const FindIdPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [foundIds, setFoundIds] = useState<string[] | null>(null);
  const [findError, setFindError] = useState("");
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FindIdFormValues>({ defaultValues: { userNm: "" } });

  const onSubmit = async (values: FindIdFormValues) => {
    setFindError("");
    setFoundIds(null);
    const result = await apiClient.post<{ data: string[] }>("/login/findId", { userNm: values.userNm });

    if (!result.success) {
      setFindError(t("account.findIdNotFound"));
      return;
    }

    setFoundIds(result.data.data ?? []);
  };

  return (
    <main className="auth-screen">
      <header className="auth-header">
        <button type="button" className="auth-back" onClick={() => navigate(-1)} aria-label={t("account.back")}>‹</button>
        <h1>{t("account.findId")}</h1>
      </header>

      {foundIds ? (
        <div className="auth-form">
          <ul className="auth-found-list">
            {foundIds.map((id) => (
              <li key={id}>{id}</li>
            ))}
          </ul>
          <Button type="button" text={t("account.backToLogin")} onClick={() => navigate("/auth")} />
        </div>
      ) : (
        <form className="auth-form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <Input
            label={t("account.name")}
            id="userNm"
            placeholder={t("account.namePlaceholder")}
            error={errors.userNm?.message}
            {...register("userNm", { required: t("account.nameRequired") })}
          />

          {findError && <p className="auth-error">{findError}</p>}

          <Button type="submit" text={isSubmitting ? t("common.saving") : t("account.findId")} disabled={isSubmitting} />
        </form>
      )}

      <div className="auth-links">
        <Link to="/auth">{t("account.backToLogin")}</Link>
      </div>
    </main>
  );
};

export default FindIdPage;
