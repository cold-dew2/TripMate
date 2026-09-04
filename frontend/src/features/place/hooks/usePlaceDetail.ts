import { apiClient } from "@/shared/api/client";
import { useQuery } from "@tanstack/react-query";
import type { PlaceDetail, PlaceAIDetail } from "@/types/place";

const usePlaceDetail = (tourId: string) => {
  const detailQuery = useQuery({
    queryKey: ["placeDetail", tourId],
    queryFn: async () => {
      const result = await apiClient.get<{ data: PlaceDetail }>(`/tourList/tourDetail/${tourId}.json`);

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
      const result = await apiClient.get<{ data: PlaceAIDetail }>(`/tourList/tourAIDetail/${tourId}.json`);

      if (!result.success) {
        throw result;
      }

      return result.data.data;
    },
    enabled: !!tourId,
  });

  return {
    data: detailQuery.data && aiDetailQuery.data ? {...detailQuery.data, ...aiDetailQuery.data} : undefined,
    isLoading: detailQuery.isLoading || aiDetailQuery.isLoading,
    isError: detailQuery.isError || aiDetailQuery.isError
  }
};

export default usePlaceDetail;