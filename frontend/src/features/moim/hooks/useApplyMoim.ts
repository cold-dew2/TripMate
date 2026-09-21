import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import type { MoimDetailResponse } from "@/types/moim";

export const useApplyMoim = (moimId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const result = await apiClient.post(`/moimList/${moimId}/apply`, {});
      if (!result.success) throw result;
      return result.data;
    },
    onSuccess: () => {
      // invalidate만 하면 버튼이 실제로 바뀌기까지 moimDetail 재조회(AI 번역 포함)가
      // 끝날 때까지 기다려야 해서, 신청이 안 된 줄 알고 다시 누르는 경우가 있었다.
      // 신청 직후 상태(대기중, roleCd='M'/stateCd='N')를 캐시에 바로 반영해 버튼을
      // 즉시 바꾸고, 서버 데이터와의 정합은 백그라운드 invalidate로 맞춘다.
      queryClient.setQueriesData<MoimDetailResponse>(
        { queryKey: ["moimDetail", moimId] },
        (prev) => (prev ? { ...prev, joinStatus: { roleCd: "M", stateCd: "N" } } : prev)
      );
      queryClient.invalidateQueries({ queryKey: ["moimDetail", moimId] });
    },
  });
};

export default useApplyMoim;
