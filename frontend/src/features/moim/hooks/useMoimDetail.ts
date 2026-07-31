import { apiClient } from "@/shared/api/client";
import { useQuery } from "@tanstack/react-query";
import type { Moim } from "@/types/moim";

const useMoimDetail = (moimId: string) => {
  return useQuery({
    queryKey: ["moimDetail", moimId],
    queryFn: async () => {
      const result = await apiClient.get<{ data: Moim }>(`/moim/details/${moimId}.json`);

      if (!result.success) {
        throw result;
      }

      return result.data.data;
    },
    enabled: !!moimId,
  });
};

export default useMoimDetail;