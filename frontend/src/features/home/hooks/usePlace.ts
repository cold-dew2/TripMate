import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/shared/api/client';
import type { PlaceResponse } from '@/types/place';

const usePlace = () => {
  return useQuery({
    queryKey: ["places"],
    queryFn: async() => {
      const result = await apiClient.get<PlaceResponse>("/trmaHome/bestTourList");

      if (!result.success) {
        throw result;
      }

      return result.data.data;
    }
  });
}

export default usePlace