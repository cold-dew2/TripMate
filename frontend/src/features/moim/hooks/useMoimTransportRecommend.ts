import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiLang } from "@/shared/utils/lang";
import type { TransportLeg, TransportRecommendResult } from "./useTransportRecommend";

// 소모임 상세 화면에서 이미 확정된 일정을 기준으로 교통편 혼잡도를 분석한다.
// 가입된 멤버만 볼 수 있는 기능이라 백엔드가 moimId+로그인 세션으로 직접 권한을 검사한다.
// useMutation을 쓰면 다른 페이지로 이동했다가 돌아왔을 때 컴포넌트가 다시 마운트되면서
// 결과가 사라져 버튼을 매번 다시 눌러야 했다. useQuery로 바꿔서 react-query 캐시에
// 결과가 남게 하고, 조회는 버튼 클릭 시 refetch()로 직접 트리거한다.
export const useMoimTransportRecommend = (moimId: string) => {
  return useQuery({
    queryKey: ["moimTransportRecommend", moimId, getApiLang()],
    queryFn: async (): Promise<TransportRecommendResult> => {
      const result = await apiClient.get<{ data: TransportLeg[]; code?: string }>(`/moimList/${moimId}/transportRecommend`, { lang: getApiLang() });
      if (!result.success) throw result;
      return { legs: result.data.data ?? [], code: result.data.code };
    },
    enabled: false,
    retry: false,
  });
};

export default useMoimTransportRecommend;
