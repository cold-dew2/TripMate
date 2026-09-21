import { useState } from 'react';
import Button from '@/shared/components/button/Button';
import DateRangePicker from '@/shared/components/datePicker/DateRangePicker';
import Input from '@/shared/components/input/Input';
import Select from '@/shared/components/select/Select';
import type { MoimCreateForm } from '@/types/moim';
import type { UseFormSetValue, UseFormWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';

interface Step2Props {
  watch: UseFormWatch<MoimCreateForm>;
  setValue: UseFormSetValue<MoimCreateForm>;
  onPrev: () => void;
  onNext: () => void;
  defaultTitle?: string;
  defaultDscr?: string;
  defaultRegion?: string;
}

// PlaceList의 지역 탭과 동일한 지역 목록을 사용한다. 일정 추천(AI) 요청 시
// 이 값을 검색 키워드로 그대로 전달해 해당 지역 관광지 위주로 추천받는다.
const REGION_OPTIONS = [
  { value: "세종", option: "세종" },
  { value: "서울", option: "서울" },
  { value: "부산", option: "부산" },
  { value: "제주", option: "제주" },
];

const Step2 = ({ watch, setValue, onNext, onPrev, defaultTitle, defaultDscr, defaultRegion }: Step2Props) => {
  const { t } = useTranslation();
  // 이전 단계에서 이미 입력해뒀다가(watch로 부모 폼에 남아있는 값) 뒤로 갔다 다시
  // 돌아온 경우 그 값을 우선 쓰고, 처음 진입(둘 다 비어있음)일 때만 prefill을 쓴다.
  const [title, setTitle] = useState(watch('moimTitle') || defaultTitle || '');
  const [dscr, setDscr] = useState(watch('moimDscr') || defaultDscr || '');
  const [dateRange, setDateRange] = useState({ start: watch('moimStartDt') || '', end: watch('moimEndDt') || '' });
  const [region, setRegion] = useState(watch('region') || defaultRegion || '');

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
            defaultValue={title}
            onChange={(event) => {
              setValue("moimTitle", event.target.value, { shouldValidate: true });
              setTitle(event.target.value);
            }}
          />
          <Input
            label={t("moimCreate.step2.info")}
            placeholder={t("moimCreate.step2.infoPlaceholder")}
            name="moimDscr"
            defaultValue={dscr}
            onChange={(event) => {
              setValue("moimDscr", event.target.value, { shouldValidate: true });
              setDscr(event.target.value);
            }}
          />
          <DateRangePicker
            label={t("moimCreate.step2.travelType")}
            onChange={handleDateChange}
            defaultStart={dateRange.start}
            defaultEnd={dateRange.end}
          />
          <Select
            label={t("moimCreate.step2.region")}
            id="region"
            name="region"
            placeholder={t("moimCreate.step2.regionPlaceholder")}
            options={REGION_OPTIONS}
            defaultValue={region}
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
            defaultValue={watch('maxMember') || 8}
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
