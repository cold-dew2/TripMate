import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";

export const useApplyMoim = (moimId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const result = await apiClient.post(`/moimList/${moimId}/apply`, {});
      if (!result.success) throw result;
      return result.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["moimDetail", moimId] });
    },
  });
};

export default useApplyMoim;
