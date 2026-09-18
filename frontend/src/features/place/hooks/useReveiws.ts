import { apiClient } from "@/shared/api/client";
import { getApiLang } from "@/shared/utils/lang";
import type { TourReview } from "@/types/reviews";
import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";

export interface CreateReviewPayload {
  tourId: string;
  reviewTitle: string;
  reviewContent: string;
  reviewScore: number;
  imageUrls: string[];
}

const useReview = (tourId: string) => {
  return useInfiniteQuery({
    queryKey: ["tourReview", tourId, getApiLang()],
    queryFn: async ({ pageParam = 1 }) => {
      const result = await apiClient.get<{ data: TourReview[] }>(
        `/tourList/tourDetailReview?tourId=${encodeURIComponent(tourId)}&page=${pageParam}&lang=${getApiLang()}`
      );

      if (!result.success) throw result;
      return result.data.data ?? [];
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage, allPages) => {
      if (!lastPage || lastPage.length < 10) return undefined;
      return allPages.length + 1;
    },
    enabled: !!tourId,
  });
};

export const useCreateReview = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateReviewPayload) => {
      const result = await apiClient.post<{ data: TourReview }>("/tourList/tourDetailReview", payload);
      if (!result.success) throw result;
      return result.data.data;
    },
    onSuccess: (_review, payload) => queryClient.invalidateQueries({ queryKey: ["tourReview", payload.tourId] }),
  });
};

export default useReview;
