import { useEffect, useState } from "react";
import { useLocation, useSearchParams } from "react-router-dom";
import "./MoimCreate.css";
import { useForm } from "react-hook-form";
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { apiClient } from '@/shared/api/client';
import PageHeader from '@/layouts/components/header/pageHeader/PageHeader';
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
}

const MoimCreate = () => {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const location = useLocation();
  const prefill = location.state as MoimCreatePrefill | null;

  const currentStep = Number(searchParams.get("step")) || 1;
  const navigate = useNavigate();

  const { setValue, watch, handleSubmit } = useForm<MoimCreateForm>({
    defaultValues: {
      moimCateData: [],
      moimPlanData: [],
      maxMember: 8,
      moimTitle: prefill?.title ?? "",
      moimDscr: prefill?.dscr ?? "",
    },
  });

  const [itemsByDay, setItemsByDay] = useState<Record<number, PlanItem[]>>({ 1: [] });
  const [activeDay, setActiveDay] = useState(1);
  const [createdMoimId, setCreatedMoimId] = useState<string | null>(null);

  const moimStartDt = watch("moimStartDt");

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

  const goToStep = (step: number) => {
    setSearchParams({ step: String(step) });
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

  const onSubmit = async (data: MoimCreateForm) => {
    const result = await apiClient.post<{ data: { moimId: string } }>("/moimList/createMoim", data);
    if (!result.success) return;
    setCreatedMoimId(result.data.data.moimId);
    goToStep(7);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
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
          setValue={setValue}
          onNext={handleNext}
          defaultThemeId={prefill?.themeId}
        />
      )}

      {currentStep === 2 && (
        <Step2
          setValue={setValue}
          onNext={handleNext}
          onPrev={handlePrev}
          defaultTitle={prefill?.title}
          defaultDscr={prefill?.dscr}
        />
      )}

      {currentStep === 3 && (
        <Step3
          watch={watch}
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
        />
      )}

      {currentStep === 7 && (
        <Step7 moimId={createdMoimId} />
      )}
    </form>
  );
};

export default MoimCreate;
