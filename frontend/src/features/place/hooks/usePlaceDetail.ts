import { apiClient } from "@/shared/api/client";
import { fetchMockJson } from "@/shared/api/mockFallback";
import { getApiLang } from "@/shared/utils/lang";
import { useQuery } from "@tanstack/react-query";
import type { PlaceDetail, PlaceAIDetail } from "@/types/place";

const usePlaceDetail = (tourId: string) => {
  const detailQuery = useQuery({
    queryKey: ["placeDetail", tourId, getApiLang()],
    queryFn: async () => {
      const result = await apiClient.get<{ data: PlaceDetail }>(
        `/tourList/tourDetail?tourId=${encodeURIComponent(tourId)}&lang=${getApiLang()}`
      );

      if (result.success && result.data.data) return result.data.data;

      // 실 API 연동이 실패했거나(네트워크/서버 오류), DB에 아직 데이터가 없어 성공 응답에
      // data가 비어있는 경우(HTTP 200 + data:null) 모두 임시로 로컬 목업 데이터로 대체한다.
      const mock = await fetchMockJson<{ data: PlaceDetail }>(
        `/data/tourList/tourDetail/${encodeURIComponent(tourId)}.json`
      );
      if (mock) return mock.data;

      throw result;
    },

    enabled: !!tourId,
  });

  const aiDetailQuery = useQuery({
    queryKey: ["placeAiDetail", tourId, getApiLang()],
    queryFn: async () => {
      const result = await apiClient.get<{ data: PlaceAIDetail }>(
        `/tourList/tourAIDetail?tourId=${encodeURIComponent(tourId)}&lang=${getApiLang()}`
      );

      if (result.success && result.data.data) return result.data.data;

      const mock = await fetchMockJson<{ data: PlaceAIDetail }>(
        `/data/tourList/tourAIDetail/${encodeURIComponent(tourId)}.json`
      );
      if (mock) return mock.data;

      throw result;
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
