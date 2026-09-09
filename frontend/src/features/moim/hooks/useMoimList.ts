import { apiClient } from "@/shared/api/client";
import type { MoimResponse } from "@/types/moim";
import { useInfiniteQuery } from "@tanstack/react-query";

const useMoimList = (category?: string) => {
  return useInfiniteQuery({
    queryKey: ["moimList", category],
    queryFn: async ({ pageParam = 1 }) => {
      const queryParams = new URLSearchParams({
        page: String(pageParam),
      });

      if (category && category !== "all") {
        queryParams.append("category", category); // 백엔드 DTO 필드명에 맞게 조정
      }

      // /moimList/moimSearch?page=1 형태 호출
      const result = await apiClient.get<MoimResponse>(
          `/moimList/moimSearch?${queryParams.toString()}`
      );

      if (!result.success) {
        throw result;
      }

      return result.data.data;
    },
    initialPageParam: 1,
    // 10개 미만으로 들어오면 마지막 페이지로 판단
    getNextPageParam: (lastPage, allPages) => {
      if (!lastPage || lastPage.length < 10) return undefined;
      return allPages.length + 1;
    },
  });
};

export default useMoimList;