import { useQuery } from "@tanstack/react-query"
import { apiClient } from "@/shared/api/client"
import { getApiLang } from "@/shared/utils/lang"
import type { MoimResponse } from "@/types/moim"
import useTranslationCatchup from "@/shared/hooks/useTranslationCatchup"

const useMoim = () => {
  const query = useQuery({
    queryKey: ["moims", getApiLang()],
    queryFn: async() => {
      const result = await apiClient.get<MoimResponse>("/trmaHome/bestMoimList", { lang: getApiLang() });

      if(!result.success) {
        throw result;
      }
      return result.data.data
    }
  })

  useTranslationCatchup(query.refetch, !query.isLoading);

  return query;
}

export default useMoim