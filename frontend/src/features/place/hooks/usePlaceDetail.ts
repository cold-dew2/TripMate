import { apiClient } from "@/shared/api/client";
import { getApiLang } from "@/shared/utils/lang";
import { useQuery } from "@tanstack/react-query";
import type { PlaceDetail, PlaceAIDetail } from "@/types/place";
import useTranslationCatchup from "@/shared/hooks/useTranslationCatchup";

// 기본 정보(이름/주소/개요/좌표 등)는 DB에서 바로 조회되어 빠르지만,
// 이용 정보(운영시간 등)는 매번 Gemini를 호출해 느리므로 별도 쿼리로 분리한다.
// 하나로 묶으면 느린 AI 조회가 끝날 때까지 기본 정보까지 화면에 못 그리게 된다.
const usePlaceDetail = (tourId: string) => {
  const query = useQuery({
    queryKey: ["placeDetail", tourId, getApiLang()],
    queryFn: async () => {
      const result = await apiClient.get<{ data: PlaceDetail }>(
        `/tourList/tourDetail?tourId=${encodeURIComponent(tourId)}&lang=${getApiLang()}`
      );

      if (!result.success) throw result;
      return result.data.data;
    },

    enabled: !!tourId,
  });

  useTranslationCatchup(query.refetch, !query.isLoading && !!tourId);

  return query;
};

export const usePlaceAIDetail = (tourId: string) => {
  return useQuery({
    queryKey: ["placeAiDetail", tourId, getApiLang()],
    queryFn: async () => {
      const result = await apiClient.get<{ data: PlaceAIDetail }>(
        `/tourList/tourAIDetail?tourId=${encodeURIComponent(tourId)}&lang=${getApiLang()}`
      );

      if (!result.success) throw result;
      return result.data.data;
    },

    enabled: !!tourId,
    // TourAPI에 없는 항목을 서버가 백그라운드에서 AI로 조사 중이면(researchPending) 잠시 뒤 다시 조회해
    // 출처가 확인된 값을 채운다. 무한 폴링을 막기 위해 최대 6번(약 24초)까지만.
    refetchInterval: (query) =>
      query.state.data?.researchPending && query.state.dataUpdateCount < 7 ? 4000 : false,
  });
};

export default usePlaceDetail;
