import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";

export interface MyReview {
  reviewTitle: string;
  reviewContent: string;
  reviewScore: number;
  userNm: string;
  profileImgUrl: string | null;
  imgUrls: string | null;
  createDt: string;
}

export type MyReviewSort = "latest" | "rating";

export const useMyReviewList = (sort: MyReviewSort = "latest") => {
  return useQuery({
    queryKey: ["myReviewList", sort],
    queryFn: async () => {
      const result = await apiClient.get<{ data: MyReview[] }>("/login/reviewList", { page: 1, sort });
      if (!result.success) throw result;
      return result.data.data ?? [];
    },
  });
};

export default useMyReviewList;
