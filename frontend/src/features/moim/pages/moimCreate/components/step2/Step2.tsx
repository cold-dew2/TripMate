import { useState } from 'react';
import Button from '@/shared/components/button/Button';
import DateRangePicker from '@/shared/components/datePicker/DateRangePicker';
import Input from '@/shared/components/input/Input';
import type { MoimCreateForm } from '@/types/moim';
import type { UseFormSetValue } from 'react-hook-form';
import { useTranslation } from 'react-i18next';

interface Step2Props {
  setValue: UseFormSetValue<MoimCreateForm>;
  onPrev: () => void;
  onNext: () => void;
  defaultTitle?: string;
  defaultDscr?: string;
}

const Step2 = ({ setValue, onNext, onPrev, defaultTitle, defaultDscr }: Step2Props) => {
  const { t } = useTranslation();
  const [title, setTitle] = useState(defaultTitle ?? '');
  const [dateRange, setDateRange] = useState({ start: '', end: '' });

  const handleDateChange = (startDate: string, endDate: string) => {
    setValue("moimStartDt", startDate);
    setValue("moimEndDt", endDate);
    setDateRange({ start: startDate, end: endDate });
  };

  const canProceed = title.trim() !== '' && !!dateRange.start && !!dateRange.end;

  return (
    <>
      <div className="create-content">
        <div>
          <Input
            label={t("moimCreate.step2.name")}
            placeholder={t("moimCreate.step2.namePlaceholder")}
            name="moimTitle"
            defaultValue={defaultTitle}
            onChange={(event) => {
              setValue("moimTitle", event.target.value, { shouldValidate: true });
              setTitle(event.target.value);
            }}
          />
          <Input
            label={t("moimCreate.step2.info")}
            placeholder={t("moimCreate.step2.infoPlaceholder")}
            name="moimDscr"
            defaultValue={defaultDscr}
            onChange={(event) => setValue("moimDscr", event.target.value, { shouldValidate: true })}
          />
          <DateRangePicker label={t("moimCreate.step2.travelType")} onChange={handleDateChange} />
          <Input
            label={t("moimCreate.step2.maxMember")}
            type="number"
            min={2}
            max={50}
            name="maxMember"
            defaultValue={8}
            onChange={(event) => setValue("maxMember", Number(event.target.value), { shouldValidate: true })}
          />
        </div>
        <div className="buttons fixed">
          <Button text={t("common.previous")} variant="secondary" onClick={onPrev} />
          <Button text={t("common.next")} onClick={onNext} disabled={!canProceed} />
        </div>
     </div>
    </>
  )
}

export default Step2 
