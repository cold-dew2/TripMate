import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiLang } from "@/shared/utils/lang";
import type { MyMoim } from "@/types/moim";
import useTranslationCatchup from "@/shared/hooks/useTranslationCatchup";

export const useMyMoim = (enabled = true) => {
  const query = useQuery({
    queryKey: ["myMoim", getApiLang()],
    queryFn: async () => {
      const result = await apiClient.get<{ data: MyMoim[] }>("/moimList/myMoim", { lang: getApiLang() });
      if (!result.success) throw result;
      return result.data.data ?? [];
    },
    enabled,
  });

  useTranslationCatchup(query.refetch, enabled && !query.isLoading);

  return query;
};

export default useMyMoim;
