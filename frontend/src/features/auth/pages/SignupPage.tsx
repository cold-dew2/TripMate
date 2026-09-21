import { useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { apiClient } from "@/shared/api/client";
import Input from "@/shared/components/input/Input";
import Select from "@/shared/components/select/Select";
import Button from "@/shared/components/button/Button";
import "./AuthPage.css";

interface SignupFormValues {
  userNm: string;
  userId: string;
  userPw: string;
  nationality: string;
  langCd: string;
}

const NATIONALITY_OPTIONS = [
  { value: "KR", option: "대한민국" },
  { value: "US", option: "United States" },
  { value: "JP", option: "日本" },
  { value: "CN", option: "中国" },
  { value: "ETC", option: "기타" },
];

const SignupPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [signupError, setSignupError] = useState("");
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SignupFormValues>({ defaultValues: { userNm: "", userId: "", userPw: "", nationality: "", langCd: "" } });

  const languageOptions = [
    { value: "ko", option: t("lang.ko") },
    { value: "en", option: t("lang.en") },
    { value: "ja", option: t("lang.jp") },
  ];

  const onSubmit = async (values: SignupFormValues) => {
    setSignupError("");
    const today = new Date().toISOString().slice(0, 10);
    const result = await apiClient.post("/login/signup", {
      userId: values.userId,
      userPw: values.userPw,
      userNm: values.userNm,
      langCd: values.langCd || "ko",
      stateCd: "Y",
      roleCd: "U",
      birthDt: today,
      genderCd: "",
      phoneNum: "",
    });

    if (!result.success) {
      // 아이디 중복처럼 원인이 명확한 실패는 그 원인을 그대로 보여준다(예전엔 무슨
      // 이유든 똑같은 "가입 실패" 문구만 떠서, 아이디가 겹친 건지 서버 오류인지
      // 사용자가 구분할 수 없었다).
      setSignupError(
        result.code === "USER_ALREADY_EXISTS" ? t("account.userAlreadyExists") : t("account.signupFailed")
      );
      return;
    }

    navigate("/auth", { replace: true, state: { signupSuccess: true } });
  };

  return (
    <main className="auth-screen auth-screen-signup">
      <header className="auth-header">
        <button type="button" className="auth-back" onClick={() => navigate(-1)} aria-label={t("account.back")}>‹</button>
        <h1>{t("account.signupTitle")}</h1>
      </header>

      <form className="auth-form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <Input
          label={t("account.name")}
          id="userNm"
          placeholder={t("account.namePlaceholder")}
          error={errors.userNm?.message}
          {...register("userNm", { required: t("account.nameRequired") })}
        />
        <Input
          label={t("account.email")}
          id="userId"
          type="email"
          placeholder={t("account.emailPlaceholder")}
          error={errors.userId?.message}
          {...register("userId", {
            required: t("account.emailRequired"),
            pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: t("account.emailInvalid") },
          })}
        />
        <Input
          label={t("account.password")}
          id="userPw"
          type="password"
          placeholder={t("account.passwordPlaceholder")}
          error={errors.userPw?.message}
          {...register("userPw", {
            required: t("account.passwordRequired"),
            minLength: { value: 8, message: t("account.passwordMin") },
          })}
        />

        <div className="auth-select-row">
          <Select
            label={t("account.nationality")}
            id="nationality"
            placeholder={t("account.nationality")}
            options={NATIONALITY_OPTIONS}
            {...register("nationality")}
          />
          <Select
            label={t("account.language")}
            id="langCd"
            placeholder={t("account.language")}
            options={languageOptions}
            {...register("langCd")}
          />
        </div>

        {signupError && <p className="auth-error">{signupError}</p>}

        <Button type="submit" text={isSubmitting ? t("common.saving") : t("account.next")} disabled={isSubmitting} />
      </form>
    </main>
  );
};

export default SignupPage;
