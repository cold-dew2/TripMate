import { apiClient } from "@/shared/api/client";
import { fetchMockJson } from "@/shared/api/mockFallback";
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

      if (result.success) return result.data.data ?? [];

      // 실 API 연동이 실패하면 임시로 로컬 목업 후기 데이터로 대체한다.
      // 목업 파일은 페이지네이션을 지원하지 않는 정적 전체 목록이므로 10개씩 잘라서 반환하고,
      // 후기 목업이 없는 관광지도 많으므로 그 경우엔 에러 대신 빈 배열로 처리한다.
      const mock = await fetchMockJson<{ data: TourReview[] }>(
        `/data/tourDetailReview/${encodeURIComponent(tourId)}.json`
      );
      if (!mock) return [];

      const start = (pageParam - 1) * 10;
      return (mock.data ?? []).slice(start, start + 10);
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
