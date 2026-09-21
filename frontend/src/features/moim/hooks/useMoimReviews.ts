import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiLang } from "@/shared/utils/lang";
import useTranslationCatchup from "@/shared/hooks/useTranslationCatchup";

export interface MoimReview {
  userNm: string;
  reviewScore: number;
  reviewContent: string;
  imgUrls: string | null;
  createDt: string;
}

export const useMoimReviews = (moimId: string) => {
  const query = useQuery({
    queryKey: ["moimReviews", moimId, getApiLang()],
    queryFn: async () => {
      const result = await apiClient.get<{ data: MoimReview[] }>(`/moimList/${moimId}/reviews`, { lang: getApiLang() });
      if (!result.success) throw result;
      return result.data.data ?? [];
    },
    enabled: !!moimId,
  });

  useTranslationCatchup(query.refetch, !query.isLoading && !!moimId);

  return query;
};

export default useMoimReviews;
