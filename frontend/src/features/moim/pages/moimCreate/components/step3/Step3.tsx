import Button from "@/shared/components/button/Button";
import ContentTitle from "@/shared/components/contentTitle/ContentTitle"
import type { MoimCreateForm } from "@/types/moim";
import type { UseFormSetValue } from "react-hook-form";
import { useTranslation } from "react-i18next";

interface Step3Props {
  setValue: UseFormSetValue<MoimCreateForm>;
  onPrev: () => void;
  onNext: () => void;
}
const Step3 = ({ setValue, onNext, onPrev }: Step3Props) => {
  const { t } = useTranslation();

  return (

    <div className="create-content">
      <ContentTitle title={t("moim.step3.title")} />

      <div className="buttons fixed">
        <Button text={t("common.previous")} variant="secondary" onClick={onPrev} />
        <Button text={t("common.next")} onClick={onNext} />
      </div>
    </div>
  )
}

export default Step3