import { apiClient } from "@/shared/api/client"
import type { PlaceResponse } from "@/types/place"
import { useQuery } from "@tanstack/react-query"

const usePlaceList = () => {
  return useQuery({
    queryKey: ["placeList"],
    queryFn: async() => {
      const result = await apiClient.get <PlaceResponse>("/places/tourList.json");

      if(!result.success) {
        throw result;
      }
      return result.data.data
    }
  })
}

export default usePlaceList