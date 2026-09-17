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
      queryClient.invalidateQueries({ queryKey: ["moimMembers", moimId] });
    },
  });
};

export default useUpdateMoimMember;
