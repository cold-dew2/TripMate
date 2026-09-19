import { useState } from 'react';
import Button from '@/shared/components/button/Button';
import DateRangePicker from '@/shared/components/datePicker/DateRangePicker';
import Input from '@/shared/components/input/Input';
import Select from '@/shared/components/select/Select';
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

// PlaceList의 지역 탭과 동일한 지역 목록을 사용한다. 일정 추천(AI) 요청 시
// 이 값을 검색 키워드로 그대로 전달해 해당 지역 관광지 위주로 추천받는다.
const REGION_OPTIONS = [
  { value: "세종", option: "세종" },
  { value: "서울", option: "서울" },
  { value: "부산", option: "부산" },
  { value: "제주", option: "제주" },
];

const Step2 = ({ setValue, onNext, onPrev, defaultTitle, defaultDscr }: Step2Props) => {
  const { t } = useTranslation();
  const [title, setTitle] = useState(defaultTitle ?? '');
  const [dscr, setDscr] = useState(defaultDscr ?? '');
  const [dateRange, setDateRange] = useState({ start: '', end: '' });
  const [region, setRegion] = useState('');

  const handleDateChange = (startDate: string, endDate: string) => {
    setValue("moimStartDt", startDate);
    setValue("moimEndDt", endDate);
    setDateRange({ start: startDate, end: endDate });
  };

  const canProceed = title.trim() !== '' && dscr.trim() !== '' && !!dateRange.start && !!dateRange.end && !!region;

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
          <Select
            label={t("moimCreate.step2.region")}
            id="region"
            name="region"
            placeholder={t("moimCreate.step2.regionPlaceholder")}
            options={REGION_OPTIONS}
            onChange={(event) => {
              setValue("region", event.target.value, { shouldValidate: true });
              setRegion(event.target.value);
            }}
          />
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
