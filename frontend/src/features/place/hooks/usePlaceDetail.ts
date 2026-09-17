import { apiClient } from "@/shared/api/client";
import { getApiLang } from "@/shared/utils/lang";
import { useQuery } from "@tanstack/react-query";
import type { PlaceDetail, PlaceAIDetail } from "@/types/place";

// .env의 VITE_API_BASE_URL이 /data(로컬 목업 JSON)를 가리킬 때는
// 쿼리스트링 대신 tourId별 정적 파일 경로로 요청한다.
const isMock = (import.meta.env.VITE_API_BASE_URL ?? "").startsWith("/data");

const usePlaceDetail = (tourId: string) => {
  const detailQuery = useQuery({
    queryKey: ["placeDetail", tourId, getApiLang()],
    queryFn: async () => {
      const endpoint = isMock
        ? `/tourList/tourDetail/${encodeURIComponent(tourId)}.json`
        : `/tourList/tourDetail?tourId=${encodeURIComponent(tourId)}&lang=${getApiLang()}`;
      const result = await apiClient.get<{ data: PlaceDetail }>(endpoint);

      if (!result.success) {
        throw result;
      }

      return result.data.data;
    },

    enabled: !!tourId,
  });

  const aiDetailQuery = useQuery({
    queryKey: ["placeAiDetail", tourId],
    queryFn: async () => {
      const endpoint = isMock
        ? `/tourList/tourAIDetail/${encodeURIComponent(tourId)}.json`
        : `/tourList/tourAIDetail?tourId=${encodeURIComponent(tourId)}`;
      const result = await apiClient.get<{ data: PlaceAIDetail }>(endpoint);

      if (!result.success) {
        throw result;
      }

      return result.data.data;
    },

    enabled: !!tourId,
  });

  return {
    data:
      detailQuery.data && aiDetailQuery.data
        ? {...detailQuery.data, ...aiDetailQuery.data}
        : undefined,
    isLoading: detailQuery.isLoading || aiDetailQuery.isLoading,
    isError: detailQuery.isError || aiDetailQuery.isError,
  };
};

export default usePlaceDetail;
