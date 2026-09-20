import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";

export const useUpdateMoimMember = (moimId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ userId, approve }: { userId: string; approve: boolean }) => {
      const result = await apiClient.put(`/moimList/${moimId}/members/${userId}`, { approve });
      if (!result.success) throw result;
      return result.data;
    },
    onSuccess: () => {
      // 승인/거절/추방으로 실제 가입자 수가 바뀌므로, 그 수를 함께 보여주는
      // 모임 상세/내 모임 관리 목록/모임 리스트 캐시도 같이 무효화한다.
      queryClient.invalidateQueries({ queryKey: ["moimMembers", moimId] });
      queryClient.invalidateQueries({ queryKey: ["moimDetail", moimId] });
      queryClient.invalidateQueries({ queryKey: ["myMoim"] });
      queryClient.invalidateQueries({ queryKey: ["moimList"] });
    },
  });
};

export default useUpdateMoimMember;
