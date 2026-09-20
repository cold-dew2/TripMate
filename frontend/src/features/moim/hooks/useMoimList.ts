import { apiClient } from "@/shared/api/client";
import { getApiLang } from "@/shared/utils/lang";
import type { MoimResponse } from "@/types/moim";
import { useInfiniteQuery } from "@tanstack/react-query";

// .env의 VITE_API_BASE_URL이 /data(로컬 목업 JSON)를 가리킬 때는 정적 파일이라
// 쿼리스트링(keyword/cateCd)을 반영하지 못하므로 프론트에서 직접 필터링한다.
const isMock = (import.meta.env.VITE_API_BASE_URL ?? "").startsWith("/data");

const useMoimList = (category?: string, keyword?: string) => {
  return useInfiniteQuery({
    queryKey: ["moimList", category, keyword ?? "", getApiLang()],
    queryFn: async ({ pageParam = 1 }) => {
      const queryParams = new URLSearchParams({
        page: String(pageParam),
        lang: getApiLang(),
      });

      if (category && category !== "all") {
        queryParams.append("cateCd", category); // 백엔드 MoimSearchRequest 필드명(cateCd)에 맞춤
      }
      if (keyword) {
        queryParams.append("keyword", keyword);
      }

      // /moimList/moimSearch?page=1 형태 호출
      const result = await apiClient.get<MoimResponse>(
          `/moimList/moimSearch?${queryParams.toString()}`
      );

      if (!result.success) {
        throw result;
      }

      let moims = result.data.data;
      if (isMock) {
        if (category && category !== "all") {
          moims = moims.filter((moim) => moim.cateCd === category || moim.region === category);
        }
        if (keyword) {
          const lower = keyword.toLowerCase();
          moims = moims.filter((moim) =>
            moim.moimTitle?.toLowerCase().includes(lower) || moim.moimDscr?.toLowerCase().includes(lower)
          );
        }
      }
      return moims;
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