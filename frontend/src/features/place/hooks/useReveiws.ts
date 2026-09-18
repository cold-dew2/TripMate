import { apiClient } from "@/shared/api/client";
import type { TourReview } from "@/types/reviews";
import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";

// .env의 VITE_API_BASE_URL이 /data(로컬 목업 JSON)를 가리킬 때는
// 쿼리스트링 대신 tourId별 정적 파일 경로로 요청한다.
const isMock = (import.meta.env.VITE_API_BASE_URL ?? "").startsWith("/data");

export interface CreateReviewPayload {
  tourId: string;
  reviewTitle: string;
  reviewContent: string;
  reviewScore: number;
  imageUrls: string[];
}

const useReview = (tourId: string) => {
  return useInfiniteQuery({
    queryKey: ["tourReview", tourId],
    queryFn: async ({ pageParam = 1 }) => {
      const endpoint = isMock
        ? `/tourDetailReview/${encodeURIComponent(tourId)}.json`
        : `/tourList/tourDetailReview?tourId=${encodeURIComponent(tourId)}&page=${pageParam}`;
      const result = await apiClient.get<{ data: TourReview[] }>(endpoint);

      if (!result.success) {
        // 목업 모드에서는 후기 파일이 없는 관광지도 많으므로 에러 대신 빈 배열로 처리
        if (isMock) return [];
        throw result;
      }

      let reviews = result.data.data ?? [];
      if (isMock) {
        // 목업 파일은 페이지네이션을 지원하지 않는 정적 전체 목록이므로 10개씩 잘라서 반환
        const start = (pageParam - 1) * 10;
        reviews = reviews.slice(start, start + 10);
      }
      return reviews;
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
