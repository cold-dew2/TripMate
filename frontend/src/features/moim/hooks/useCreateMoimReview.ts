import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";

export interface CreateMoimReviewPayload {
  reviewContent: string;
  reviewScore: number;
  imageUrls: string[];
}

export const useCreateMoimReview = (moimId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateMoimReviewPayload) => {
      const result = await apiClient.post(`/moimList/${moimId}/review`, payload);
      if (!result.success) throw result;
      return result.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["moimReviews", moimId] });
    },
  });
};

export default useCreateMoimReview;
