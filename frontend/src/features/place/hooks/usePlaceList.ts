import { apiClient } from "@/shared/api/client";
import { getApiLang } from "@/shared/utils/lang";
import type { PlaceResponse } from "@/types/place";
import { useInfiniteQuery } from "@tanstack/react-query";

// .env의 VITE_API_BASE_URL이 /data(로컬 목업 JSON)를 가리킬 때는 정적 파일이라
// 쿼리스트링(keyword/cateCd)을 반영하지 못하므로 프론트에서 직접 필터링한다.
const isMock = (import.meta.env.VITE_API_BASE_URL ?? "").startsWith("/data");

export const usePlaceList = (category: string, keyword?: string) => {
  return useInfiniteQuery({
    queryKey: ["placeList", category, keyword ?? "", getApiLang()],
    queryFn: async ({ pageParam = 1 }) => {
      const cateParam = category !== "all" ? category : "";

      // 쿼리 스트링 조합 (?page=1&cateCd=...&keyword=...&lang=...)
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

      // 백엔드 엔드포인트: /tourList/tourSearch?page=1
      const result = await apiClient.get<PlaceResponse>(
          `/tourList/tourSearch?${queryParams.toString()}`
      );

      if (!result.success) {
        throw result;
      }

      let places = result.data.data;
      if (isMock) {
        if (cateParam) {
          places = places.filter((place) => place.cateCd === cateParam);
        }
        if (keyword) {
          const lower = keyword.toLowerCase();
          places = places.filter((place) =>
            place.tourNm?.toLowerCase().includes(lower)
            || place.roadAddr?.toLowerCase().includes(lower)
            || place.sidoNm?.toLowerCase().includes(lower)
            || place.sggNm?.toLowerCase().includes(lower)
          );
        }
      }
      return places;
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