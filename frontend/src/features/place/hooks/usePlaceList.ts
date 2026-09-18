import { apiClient } from "@/shared/api/client";
import { fetchMockJson } from "@/shared/api/mockFallback";
import { getApiLang } from "@/shared/utils/lang";
import type { Place, PlaceResponse } from "@/types/place";
import { useInfiniteQuery } from "@tanstack/react-query";

// 목업 데이터는 페이지네이션/검색을 지원하지 않는 정적 전체 목록이므로 프론트에서 직접 필터링한다.
const filterPlaces = (places: Place[], cateParam: string, keyword?: string) => {
  let filtered = places;
  if (cateParam) {
    filtered = filtered.filter((place) => place.cateCd === cateParam);
  }
  if (keyword) {
    const lower = keyword.toLowerCase();
    filtered = filtered.filter((place) =>
      place.tourNm?.toLowerCase().includes(lower)
      || place.roadAddr?.toLowerCase().includes(lower)
      || place.sidoNm?.toLowerCase().includes(lower)
      || place.sggNm?.toLowerCase().includes(lower)
    );
  }
  return filtered;
};

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

      // DB에 아직 데이터가 없으면 성공 응답이어도 첫 페이지가 빈 배열로 온다.
      // 이 경우도 실패와 동일하게 목업으로 대체하되, 2페이지 이후의 정상적인
      // "더 이상 결과 없음"(빈 배열)까지 목업으로 덮어쓰지 않도록 첫 페이지에만 적용한다.
      if (result.success && !(pageParam === 1 && result.data.data.length === 0)) {
        return result.data.data;
      }

      // 실 API 연동이 실패했거나 DB가 비어있으면 임시로 로컬 목업 관광지 목록으로 대체한다.
      const mock = await fetchMockJson<PlaceResponse>(`/data/places/tourList.json`);
      if (mock) {
        const filtered = filterPlaces(mock.data, cateParam, keyword);
        const start = (pageParam - 1) * 10;
        return filtered.slice(start, start + 10);
      }

      throw result;
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
