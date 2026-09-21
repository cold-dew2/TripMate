import { useEffect } from "react";
import { getApiLang } from "@/shared/utils/lang";

// 백엔드가 번역 대기 예산(0.5초)을 넘기면 일단 한국어 원문으로 먼저 응답한다(화면을
// 빠르게 띄우기 위해). 번역 자체는 서버에서 백그라운드로 계속 돌아 캐시에 저장되므로,
// 몇 초 뒤 조용히 한 번 더 불러오면 그 사이 끝난 번역으로 자연스럽게 바뀐다.
// 한국어 화면이면 애초에 번역할 게 없으니 아무것도 하지 않는다.
export const useTranslationCatchup = (refetch: () => void, enabled: boolean) => {
  useEffect(() => {
    if (!enabled || getApiLang() === "ko") return;

    const timers = [2500, 6000].map((delay) => window.setTimeout(() => refetch(), delay));
    return () => timers.forEach((timer) => window.clearTimeout(timer));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled]);
};

export default useTranslationCatchup;
