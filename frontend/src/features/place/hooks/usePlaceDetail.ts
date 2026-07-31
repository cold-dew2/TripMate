import { apiClient } from "@/shared/api/client";
import { useQuery } from "@tanstack/react-query";
import type { Place } from "@/types/place";

const usePlaceDetail = (tourId: string) => {
  return useQuery({
    queryKey: ["placeDetail", tourId],
    queryFn: async () => {
      const result = await apiClient.get<{ data: Place }>(`/moim/details/${tourId}.json`);

      if (!result.success) {
        throw result;
      }

      return result.data.data;
    },
    enabled: !!tourId,
  });
};

export default usePlaceDetail;