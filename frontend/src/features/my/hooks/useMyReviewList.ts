import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiLang } from "@/shared/utils/lang";
import useTranslationCatchup from "@/shared/hooks/useTranslationCatchup";

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
  const query = useQuery({
    queryKey: ["myReviewList", sort, getApiLang()],
    queryFn: async () => {
      const result = await apiClient.get<{ data: MyReview[] }>("/login/reviewList", { page: 1, sort, lang: getApiLang() });
      if (!result.success) throw result;
      return result.data.data ?? [];
    },
  });

  useTranslationCatchup(query.refetch, !query.isLoading);

  return query;
};

export default useMyReviewList;
