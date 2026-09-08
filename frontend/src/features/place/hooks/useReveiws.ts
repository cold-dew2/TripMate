import { apiClient } from "@/shared/api/client";
import type { TourReview } from "@/types/reviews";
import { useQuery } from "@tanstack/react-query";

const useReview = (tourId: string) => {
  const reviewQuery = useQuery({
    queryKey: ["tourReview", tourId],
    queryFn: async () => {
      const result = await apiClient.get<{ data: TourReview[] }>(`/tourDetailReview.json?tourId=${tourId}`);

      if (!result.success) {
        throw result;
      }

      return result.data.data;
    },
    enabled: !!tourId,
  });

  return {
    data: reviewQuery.data,
    isLoading: reviewQuery.isLoading,
    isError: reviewQuery.isError,
  };
};

export default useReview;