import Button from '@/shared/components/button/Button';
import Input from '@/shared/components/input/Input';
import type { MoimCreateForm } from '@/types/moim';
import type { UseFormSetValue } from 'node_modules/react-hook-form/dist/types/form';
import React from 'react'
import { useTranslation } from 'react-i18next';

interface Step1Props {
  setValue: UseFormSetValue<MoimCreateForm>;
  onPrev: () => void;
  onNext: () => void;
}

const Step2 = ({ setValue, onNext, onPrev }: Step1Props) => {
  const { t } = useTranslation();
  return (
    <>
      <div className="create-content">
        <div>
          <Input label={t("moim.create.step2.name")} placeholder={t("moim.create.step2.name.placeholder")} name="step2Form" />
          <Input label={t("moim.create.step2.info")} placeholder={t("moim.create.step2.info.placeholder")} name="step2Form" />
        </div>
        <div className="buttons fixed">
          <Button text={t("이전")} variant="secondary" onClick={onPrev} />
          <Button text={t("다음")} onClick={onNext} />
        </div>
     </div>
    </>
  )
}

export default Step2 