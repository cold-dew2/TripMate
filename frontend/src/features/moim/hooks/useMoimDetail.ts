import { apiClient } from "@/shared/api/client";
import { getApiLang } from "@/shared/utils/lang";
import { useQuery } from "@tanstack/react-query";
import type { MoimDetailResponse } from "@/types/moim";

// .env의 VITE_API_BASE_URL이 /data(로컬 목업 JSON)를 가리킬 때는
// 쿼리스트링 대신 moimId별 정적 파일 경로로 요청한다.
const isMock = (import.meta.env.VITE_API_BASE_URL ?? "").startsWith("/data");

const useMoimDetail = (moimId: string) => {
  return useQuery({
    queryKey: ["moimDetail", moimId, getApiLang()],
    queryFn: async () => {
      const endpoint = isMock
        ? `/moimList/moimDetail/${encodeURIComponent(moimId)}.json`
        : `/moimList/moimDetail?moimId=${encodeURIComponent(moimId)}&lang=${getApiLang()}`;
      const result = await apiClient.get<MoimDetailResponse>(endpoint);

      if (!result.success) {
        throw result;
      }

      return result.data;
    },
    enabled: !!moimId,
  });
};

export default useMoimDetail;
