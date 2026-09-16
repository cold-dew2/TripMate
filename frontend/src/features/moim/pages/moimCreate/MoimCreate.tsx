import { useSearchParams } from "react-router-dom";
import "./MoimCreate.css";
import { useForm } from "react-hook-form";

import type { MoimCreateForm } from "@/types/moim";

import Step1 from "./components/step1/Step1";
import Step2 from "./components/step2/Step2";
import Step3 from "./components/step3/Step3";

const MoimCreate = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  const currentStep = Number(searchParams.get("step")) || 1;

  const { setValue, handleSubmit } = useForm<MoimCreateForm>();

  const onSubmit = (data: MoimCreateForm) => {
    console.log(data);
  };

  const handleNext = () => {
    setSearchParams({
      step: String(currentStep + 1),
    });
  };

  const handlePrev = () => {
    setSearchParams({
      step: String(currentStep - 1),
    });
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      {currentStep === 1 && (
        <Step1
          setValue={setValue}
          onNext={handleNext}
        />
      )}

      {currentStep === 2 && (
        <Step2
          setValue={setValue}
          onNext={handleNext}
          onPrev={handlePrev}
        />
      )}

      {currentStep === 3 && (
        <Step3
          setValue={setValue}
          onNext={handleNext}
          onPrev={handlePrev}
        />
      )}
    </form>
  );
};

export default MoimCreate;