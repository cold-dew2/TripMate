import { apiClient } from "@/shared/api/client"
import type { MoimResponse } from "@/types/moim"
import { useQuery } from "@tanstack/react-query"

const useMoimList = () => {
  return useQuery({
    queryKey: ["moimList"],
    queryFn: async() => {
      const result = await apiClient.get <MoimResponse>("/moim/moimList.json");

      if(!result.success) {
        throw result;
      }
      return result.data.data
    }
  })
}

export default useMoimList