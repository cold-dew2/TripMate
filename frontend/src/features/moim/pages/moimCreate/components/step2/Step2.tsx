import Button from '@/shared/components/button/Button';
import DateRangePicker from '@/shared/components/datePicker/DateRangePicker';
import Input from '@/shared/components/input/Input';
import type { MoimCreateForm } from '@/types/moim';
import type { UseFormSetValue } from 'node_modules/react-hook-form/dist/types/form';
import React from 'react'
import { useTranslation } from 'react-i18next';

interface Step2Props {
  setValue: UseFormSetValue<MoimCreateForm>;
  onPrev: () => void;
  onNext: () => void;
}

const Step2 = ({ setValue, onNext, onPrev }: Step2Props) => {
  const { t } = useTranslation();

  const handleDateChange = (startDate: string, endDate: string) => { 
    setValue("moimStartDt", startDate); 
    setValue("moimEndDt", endDate); 
  };
  return (
    <>
      <div className="create-content">
        <div>
          <Input label={t("moim.create.step2.name")} placeholder={t("moim.create.step2.name.placeholder")} name="step2Title" />
          <Input label={t("moim.create.step2.info")} placeholder={t("moim.create.step2.info.placeholder")} name="step2Desc" />
          <DateRangePicker label={t("common.travelPeriod")}onChange={handleDateChange} />
        </div>
        <div className="buttons fixed">
          <Button text={t("common.previous")} variant="secondary" onClick={onPrev} />
          <Button text={t("common.next")} onClick={onNext} />
        </div>
     </div>
    </>  
  )
}

export default Step2 