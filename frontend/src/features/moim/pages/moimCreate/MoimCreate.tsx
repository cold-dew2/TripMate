import { useEffect, useRef, useState } from "react";
import { useLocation, useSearchParams } from "react-router-dom";
import "./MoimCreate.css";
import { useForm } from "react-hook-form";
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { apiClient } from '@/shared/api/client';
import PageHeader from '@/layouts/components/header/pageHeader/PageHeader';
import { useAlert } from '@/shared/contexts/AlertContext';
import { addDays } from '@/shared/utils/date';

import type { MoimCreateForm, MoimCreatePrefill } from "@/types/moim";

import Step1 from "./components/step1/Step1";
import Step2 from "./components/step2/Step2";
import Step3 from "./components/step3/Step3";
import Step4 from "./components/step4/Step4";
import Step5 from "./components/step5/Step5";
import Step6 from "./components/step6/Step6";
import Step7 from "./components/step7/Step7";

const TOTAL_STEPS = 7;

const isMock = (import.meta.env.VITE_API_BASE_URL ?? "").startsWith("/data");

export interface PlanItem {
  id: string;
  time: string;
  placeName: string;
  tourId: string;
  imageUrl?: string;
  sidoNm?: string;
  sggNm?: string;
  roadAddr?: string;
}

const MoimCreate = () => {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const location = useLocation();
  // setSearchParams로 step을 바꿀 때마다 새 history 엔트리가 만들어지면서
  // location.state가 사라지므로(리액트 라우터 기본 동작), 세종 코스 등에서 넘어온
  // prefill 데이터를 진입 시점에 한 번만 캡처해 단계 이동과 무관하게 유지한다.
  const [prefill] = useState(() => location.state as MoimCreatePrefill | null);

  const currentStep = Number(searchParams.get("step")) || 1;
  const navigate = useNavigate();

  const { setValue, watch, handleSubmit, formState: { isSubmitting } } = useForm<MoimCreateForm>({
    defaultValues: {
      moimCateData: [],
      moimPlanData: [],
      maxMember: 8,
      moimTitle: prefill?.title ?? "",
      moimDscr: prefill?.dscr ?? "",
      region: prefill?.region ?? "",
    },
  });

  const [itemsByDay, setItemsByDay] = useState<Record<number, PlanItem[]>>({ 1: [] });
  const [activeDay, setActiveDay] = useState(1);
  const [createdMoimId, setCreatedMoimId] = useState<string | null>(null);
  const { showAlert } = useAlert();

  const moimCateData = watch("moimCateData");
  const moimTitle = watch("moimTitle");
  const moimDscr = watch("moimDscr");
  const moimStartDt = watch("moimStartDt");
  const moimEndDt = watch("moimEndDt");
  const region = watch("region");

  // DB에는 모임명/소개/여행기간이 NOT NULL이라, 이 값들이 안 채워진 채로(예: 주소창에
  // ?step=6을 직접 입력하는 식으로) 뒷 단계까지 건너뛰어 등록을 시도하면 서버에서
  // "Column 'MOIM_START_DT' cannot be null" 같은 오류로 등록 자체가 실패한다.
  // 그래서 이전 단계가 덜 채워진 상태로는 다음 단계에 아예 진입하지 못하게 막는다.
  const isStep1Complete = (moimCateData?.length ?? 0) > 0;
  const isStep2Complete = isStep1Complete
    && !!moimTitle?.trim()
    && !!moimDscr?.trim()
    && !!moimStartDt
    && !!moimEndDt
    && !!region;

  useEffect(() => {
    if (currentStep >= 2 && !isStep1Complete) {
      setSearchParams({ step: "1" });
      return;
    }
    if (currentStep >= 3 && !isStep2Complete) {
      setSearchParams({ step: "2" });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentStep, isStep1Complete, isStep2Complete]);

  useEffect(() => {
    const courseStops = prefill?.courseStops;
    if (!courseStops || courseStops.length === 0) return;

    let cancelled = false;

    (async () => {
      const items: PlanItem[] = [];
      for (let index = 0; index < courseStops.length; index++) {
        const stop = courseStops[index];
        let tourId = `COURSE${index}-${Date.now()}`;

        if (!isMock) {
          const result = await apiClient.post<{ data: { tourId: string } }>("/tourList/customTour", {
            tourNm: stop.name,
            roadAddr: stop.address,
          });
          if (result.success) tourId = result.data.data.tourId;
        }

        const hour = (10 + index * 3) % 24;
        items.push({
          id: `${tourId}-${index}`,
          time: `${String(hour).padStart(2, "0")}:00`,
          placeName: stop.name,
          tourId,
          sidoNm: stop.sidoNm,
          sggNm: stop.sggNm,
          roadAddr: stop.address,
        });
      }
      if (!cancelled) setItemsByDay({ 1: items });
    })();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const flattened = Object.entries(itemsByDay).flatMap(([day, items]) =>
      items.map((item) => ({
        startDt: addDays(moimStartDt, Number(day) - 1),
        rmks: item.time,
        tourId: item.tourId,
      }))
    );
    setValue("moimPlanData", flattened);
  }, [itemsByDay, moimStartDt, setValue]);

  // replace: true로 바꾸지 않으면 스텝을 옮길 때마다(1→2→...→7) 브라우저 히스토리에
  // 엔트리가 하나씩 쌓인다. 이 마법사 안에서의 "이전" 버튼은 브라우저 뒤로가기가 아니라
  // handleHeaderBack이 직접 처리하므로 이 히스토리 엔트리들은 원래 쓸모가 없는데, 생성
  // 완료 후 상세 화면으로 넘어간 다음 뒤로가기를 누르면 모임 목록이 아니라 이 마법사의
  // 이전 스텝들을 하나씩 거슬러 올라가는 문제가 있었다.
  const goToStep = (step: number) => {
    setSearchParams({ step: String(step) }, { replace: true });
  };

  const handleNext = () => goToStep(currentStep + 1);
  const handlePrev = () => goToStep(currentStep - 1);

  const handleHeaderBack = () => {
    if (currentStep > 1) {
      handlePrev();
    } else {
      navigate("/moimList");
    }
  };

  const openSpotPick = (day: number) => {
    setActiveDay(day);
    goToStep(4);
  };

  const isSubmittingRef = useRef(false);

  const onSubmit = async (data: MoimCreateForm) => {
    // isSubmitting 갱신은 리렌더를 거치므로, 그 사이에 들어오는 추가 클릭(연타)까지
    // 막으려면 상태보다 즉시 반영되는 ref로 한 번 더 막아야 한다.
    if (isSubmittingRef.current) return;
    isSubmittingRef.current = true;

    try {
      const result = await apiClient.post<{ data: { moimId: string } }>("/moimList/createMoim", data);

      if (!result.success) {
        // 실패 원인을 그대로 보여줘야 "왜 안 되는지" 바로 알 수 있다(로그인 필요/AI 사용 불가/서버 오류 등).
        if (result.code === "NEED_LOGIN") {
          showAlert(t("moimCreate.step6.needLogin"));
          navigate("/auth");
        } else {
          showAlert(result.message || t("moimCreate.step6.registerError"));
        }
        return;
      }

      if (!result.data.data) {
        showAlert(t("moimCreate.step6.registerError"));
        return;
      }

      setCreatedMoimId(result.data.data.moimId);
      goToStep(7);
    } finally {
      isSubmittingRef.current = false;
    }
  };

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      onKeyDown={(event) => {
        // 여러 단계(검색창, 제목/설명 입력 등)가 이 하나의 form 안에 있다 보니,
        // 어느 input에서든 엔터를 누르면 마지막 단계의 "등록하기"처럼 form 전체가
        // 제출돼 버린다. 실제 제출 버튼(Step6의 등록 버튼) 클릭이 아닌 이상 막는다.
        if (event.key === "Enter" && (event.target as HTMLElement).tagName === "INPUT") {
          event.preventDefault();
        }
      }}
    >
      {currentStep < 7 && (
        <PageHeader
          contentTitle={t("moimCreate.title")}
          current={currentStep}
          total={TOTAL_STEPS}
          onBack={handleHeaderBack}
        />
      )}

      {currentStep === 1 && (
        <Step1
          watch={watch}
          setValue={setValue}
          onNext={handleNext}
          defaultThemeId={prefill?.themeId}
        />
      )}

      {currentStep === 2 && (
        <Step2
          watch={watch}
          setValue={setValue}
          onNext={handleNext}
          onPrev={handlePrev}
          defaultTitle={prefill?.title}
          defaultDscr={prefill?.dscr}
          defaultRegion={prefill?.region}
        />
      )}

      {currentStep === 3 && (
        <Step3
          watch={watch}
          setValue={setValue}
          itemsByDay={itemsByDay}
          setItemsByDay={setItemsByDay}
          onAddDay={openSpotPick}
          onNext={() => goToStep(5)}
          onPrev={handlePrev}
        />
      )}

      {currentStep === 4 && (
        <Step4
          day={activeDay}
          watch={watch}
          items={itemsByDay[activeDay] ?? []}
          onAddItem={(item) =>
            setItemsByDay((prev) => ({ ...prev, [activeDay]: [...(prev[activeDay] ?? []), item] }))
          }
          onRemoveItem={(tourId) =>
            setItemsByDay((prev) => ({ ...prev, [activeDay]: (prev[activeDay] ?? []).filter((item) => item.tourId !== tourId) }))
          }
          onDone={() => goToStep(3)}
        />
      )}

      {currentStep === 5 && (
        <Step5
          watch={watch}
          itemsByDay={itemsByDay}
          onEditPlan={() => goToStep(3)}
          onNext={() => goToStep(6)}
        />
      )}

      {currentStep === 6 && (
        <Step6
          watch={watch}
          setValue={setValue}
          itemsByDay={itemsByDay}
          onPrev={() => goToStep(5)}
          isSubmitting={isSubmitting}
        />
      )}

      {currentStep === 7 && (
        <Step7 moimId={createdMoimId} />
      )}
    </form>
  );
};

export default MoimCreate;
