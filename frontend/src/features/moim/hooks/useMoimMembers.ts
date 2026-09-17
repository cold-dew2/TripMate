import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import type { MoimMember } from "@/types/moim";

export const useMoimMembers = (moimId: string) => {
  return useQuery({
    queryKey: ["moimMembers", moimId],
    queryFn: async () => {
      const result = await apiClient.get<{ data: MoimMember[] }>(`/moimList/${moimId}/members`);
      if (!result.success) throw result;
      return result.data.data ?? [];
    },
    enabled: !!moimId,
  });
};

export default useMoimMembers;
