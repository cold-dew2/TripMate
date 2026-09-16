import { apiClient } from "@/shared/api/client";
import { useQuery } from "@tanstack/react-query";
import type { MoimDetailResponse } from "@/types/moim";

const useMoimDetail = (moimId: string) => {
  return useQuery({
    queryKey: ["moimDetail", moimId],
    queryFn: async () => {
      // const result = await apiClient.get<{ data: MoimDetailResponse }>(`/moimList/moimDetail.json?moimId=${moimId}`);
      const result = await apiClient.get<{ data: Moim }>(`/moimList/moimDetail/${moimId}.json`);

      if (!result.success) {
        throw result;
      }

      return result;
    },
    enabled: !!moimId,
  });
};

export default useMoimDetail;