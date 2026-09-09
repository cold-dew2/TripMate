import { apiClient } from "@/shared/api/client";
import type { PlaceResponse } from "@/types/place";
import { useInfiniteQuery } from "@tanstack/react-query";

export const usePlaceList = (category: string) => {
  return useInfiniteQuery({
    queryKey: ["placeList", category],
    queryFn: async ({ pageParam = 1 }) => {
      const cateParam = category !== "all" ? category : "";

      // 쿼리 스트링 조합 (?page=1&cateNm=서울)
      const queryParams = new URLSearchParams({
        page: String(pageParam),
      });

      if (cateParam) {
        queryParams.append("cateNm", cateParam); // 백엔드 DTO 필드명에 맞게 설정
      }

      // 백엔드 엔드포인트: /tourList/tourSearch?page=1
      const result = await apiClient.get<PlaceResponse>(
          `/tourList/tourSearch?${queryParams.toString()}`
      );

      if (!result.success) {
        throw result;
      }
      return result.data.data;
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage, allPages) => {
      // 10개 미만으로 돌아오면 다음 페이지가 없다고 판단 (offset 10 기준)
      if (!lastPage || lastPage.length < 10) return undefined;
      return allPages.length + 1;
    },
  });
};

export default usePlaceList;