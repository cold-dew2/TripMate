import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";

export interface MoimReview {
  userNm: string;
  reviewScore: number;
  reviewContent: string;
  imgUrls: string | null;
  createDt: string;
}

export const useMoimReviews = (moimId: string) => {
  return useQuery({
    queryKey: ["moimReviews", moimId],
    queryFn: async () => {
      const result = await apiClient.get<{ data: MoimReview[] }>(`/moimList/${moimId}/reviews`);
      if (!result.success) throw result;
      return result.data.data ?? [];
    },
    enabled: !!moimId,
  });
};

export default useMoimReviews;
