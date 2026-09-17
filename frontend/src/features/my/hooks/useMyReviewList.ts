import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";

export interface MyReview {
  reviewTitle: string;
  reviewContent: string;
  reviewScore: number;
  userNm: string;
  imgUrls: string | null;
  createDt: string;
}

export const useMyReviewList = () => {
  return useQuery({
    queryKey: ["myReviewList"],
    queryFn: async () => {
      const result = await apiClient.get<{ data: MyReview[] }>("/login/reviewList", { page: 1 });
      if (!result.success) throw result;
      return result.data.data ?? [];
    },
  });
};

export default useMyReviewList;
