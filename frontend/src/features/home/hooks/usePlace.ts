import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/shared/api/client';
import { getApiLang } from '@/shared/utils/lang';
import type { PlaceResponse } from '@/types/place';

const usePlace = () => {
  return useQuery({
    queryKey: ["places", getApiLang()],
    queryFn: async() => {
      const result = await apiClient.get<PlaceResponse>("/trmaHome/bestTourList", { lang: getApiLang() });

      if (!result.success) {
        throw result;
      }

      return result.data.data;
    }
  });
}

export default usePlace