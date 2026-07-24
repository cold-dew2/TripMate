import { useQuery } from "@tanstack/react-query"
import { apiClient } from "../api/client"
import type { MoimResponse } from "@/types/moim"

const useMoim = () => {
  return useQuery({
    queryKey: ["moims"],
    queryFn: async() => {
      const result = await apiClient.get<MoimResponse>("/trmaHome/bestMoimList.json");

      if(!result.success) {
        throw result;
      }
      return result.data.data
    }
  })
}

export default useMoim