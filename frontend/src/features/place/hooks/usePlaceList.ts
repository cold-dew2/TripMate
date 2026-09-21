import { apiClient } from "@/shared/api/client";
import { getApiLang } from "@/shared/utils/lang";
import type { PlaceResponse } from "@/types/place";
import { useInfiniteQuery } from "@tanstack/react-query";
import useTranslationCatchup from "@/shared/hooks/useTranslationCatchup";

export const usePlaceList = (category: string, keyword?: string, region?: string) => {
  const query = useInfiniteQuery({
    queryKey: ["placeList", category, keyword ?? "", region ?? "", getApiLang()],
    queryFn: async ({ pageParam = 1 }) => {
      const cateParam = category !== "all" ? category : "";

      // 쿼리 스트링 조합 (?page=1&cateCd=...&keyword=...&region=...&lang=...)
      const queryParams = new URLSearchParams({
        page: String(pageParam),
        lang: getApiLang(),
      });

      if (cateParam) {
        queryParams.append("cateCd", cateParam); // 백엔드 TourSearchRequest 필드명(cateCd)에 맞춤
      }
      if (keyword) {
        queryParams.append("keyword", keyword);
      }
      if (region) {
        // 도로명주소가 그 지역명으로 시작하는 것만 정확히 거르는 전용 필터(keyword 재사용 시
        // "세종대로"/"세종대왕" 같은 우연한 일치가 섞이는 문제가 있었다).
        queryParams.append("region", region);
      }

      // 백엔드 엔드포인트: /tourList/tourSearch?page=1
      const result = await apiClient.get<PlaceResponse>(
          `/tourList/tourSearch?${queryParams.toString()}`
      );

      if (!result.success) throw result;
      return result.data.data;
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage, allPages) => {
      // 10개 미만으로 돌아오면 다음 페이지가 없다고 판단 (offset 10 기준)
      if (!lastPage || lastPage.length < 10) return undefined;
      return allPages.length + 1;
    },
  });

  useTranslationCatchup(query.refetch, !query.isLoading);

  return query;
};

export default usePlaceList;
