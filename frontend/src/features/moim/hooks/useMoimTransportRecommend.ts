import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import type { TransportLeg } from "./useTransportRecommend";

// 소모임 상세 화면에서 이미 확정된 일정을 기준으로 교통편 혼잡도를 분석한다.
// 가입된 멤버만 볼 수 있는 기능이라 백엔드가 moimId+로그인 세션으로 직접 권한을 검사한다.
export const useMoimTransportRecommend = (moimId: string) => {
  return useMutation({
    mutationFn: async () => {
      const result = await apiClient.get<{ data: TransportLeg[] }>(`/moimList/${moimId}/transportRecommend`);
      if (!result.success) throw result;
      return result.data.data ?? [];
    },
  });
};

export default useMoimTransportRecommend;
