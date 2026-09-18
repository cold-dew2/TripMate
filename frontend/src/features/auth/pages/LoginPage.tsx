import { useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { apiClient } from "@/shared/api/client";
import Input from "@/shared/components/input/Input";
import Button from "@/shared/components/button/Button";
import Checkbox from "@/shared/components/checkbox/Checkbox";
import "./AuthPage.css";

interface LoginFormValues {
  userId: string;
  userPw: string;
  rememberMe: boolean;
}

interface LoginData {
  token: string;
}

const LoginPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const signupSuccess = Boolean((location.state as { signupSuccess?: boolean } | null)?.signupSuccess);
  const [loginError, setLoginError] = useState("");
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({ defaultValues: { userId: "", userPw: "", rememberMe: false } });

  const onSubmit = async (values: LoginFormValues) => {
    setLoginError("");
    const result = await apiClient.post<LoginData>("/login/login", {
      userId: values.userId,
      userPw: values.userPw,
    });

    if (!result.success || !result.data?.token) {
      setLoginError(t("account.loginFailed"));
      return;
    }

    if (values.rememberMe) {
      localStorage.setItem("accessToken", result.data.token);
    } else {
      sessionStorage.setItem("accessToken", result.data.token);
    }

    navigate("/", { replace: true });
  };

  return (
    <main className="auth-screen">
      <div className="auth-logo">
        <span className="auth-logo-icon" aria-hidden="true">📍</span>
      </div>
      <h1 className="auth-title">{t("account.loginTitle")}</h1>
      <p className="auth-subtitle">{t("account.loginSubtitle")}</p>

      {signupSuccess && <p className="auth-success">{t("account.signupSuccess")}</p>}

      <form className="auth-form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <Input
          label={t("account.email")}
          blind
          id="userId"
          placeholder={t("account.emailPlaceholder")}
          error={errors.userId?.message}
          {...register("userId", { required: t("account.emailRequired") })}
        />
        <Input
          label={t("account.password")}
          blind
          id="userPw"
          type="password"
          placeholder={t("account.passwordPlaceholder")}
          error={errors.userPw?.message}
          {...register("userPw", { required: t("account.passwordRequired") })}
        />

        <div className="auth-remember">
          <Checkbox id="rememberMe" label={t("account.rememberMe")} {...register("rememberMe")} />
        </div>

        {loginError && <p className="auth-error">{loginError}</p>}

        <Button type="submit" text={isSubmitting ? t("common.saving") : t("account.login")} disabled={isSubmitting} />
      </form>

      <div className="auth-links">
        <span>{t("account.findId")}</span>
        <span aria-hidden="true">|</span>
        <span>{t("account.findPw")}</span>
        <span aria-hidden="true">|</span>
        <Link to="/auth/signup" className="auth-link-signup">{t("account.signup")}</Link>
      </div>
    </main>
  );
};

export default LoginPage;
