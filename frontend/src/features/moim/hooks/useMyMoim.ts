import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiLang } from "@/shared/utils/lang";
import type { MyMoim } from "@/types/moim";

export const useMyMoim = (enabled = true) => {
  return useQuery({
    queryKey: ["myMoim", getApiLang()],
    queryFn: async () => {
      const result = await apiClient.get<{ data: MyMoim[] }>("/moimList/myMoim", { lang: getApiLang() });
      if (!result.success) throw result;
      return result.data.data ?? [];
    },
    enabled,
  });
};

export default useMyMoim;
