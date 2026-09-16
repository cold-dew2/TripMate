import Button from '@/shared/components/button/Button';
import Input from '@/shared/components/input/Input';
import NoticeBox from '@/shared/components/noticeBox/NoticeBox';
import { useTranslation } from 'react-i18next';
import "./SafetyReport.css"

const SafetyReport = () => {
  const { t } = useTranslation();

  return (
    <>
      <NoticeBox desc={t("safetyReport.notice")} type="red" />

      <form className="form">
        <Input label={t("safetyReport.moimName")} disabled name="moinName"/>


        <Button text={t("common.declaration")} variant="negative" size="lg" />
      </form>

    </>

  )
}

export default SafetyReport