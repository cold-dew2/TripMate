import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiLang } from "@/shared/utils/lang";
import type { MyMoim } from "@/types/moim";
import useTranslationCatchup from "@/shared/hooks/useTranslationCatchup";

export interface LanguageCard {
  langCd: string;
  langNm: string;
  levelNm: string;
}

export interface PublicProfile {
  userId: string;
  userNm: string;
  areaNm: string;
  description: string;
  profileImageUrl: string;
  rating: number;
  reviewCount: number;
  joinDt: string;
  languages: LanguageCard[];
}

export interface PublicReview {
  reviewTitle: string;
  reviewContent: string;
  reviewScore: number;
  userNm: string;
  imgUrls: string | null;
  createDt: string;
}

export interface PublicProfileResult {
  data: PublicProfile;
  moims: MyMoim[];
  reviews: PublicReview[];
}

export const usePublicProfile = (userId: string) => {
  const query = useQuery({
    queryKey: ["publicProfile", userId, getApiLang()],
    queryFn: async () => {
      const result = await apiClient.get<PublicProfileResult>(`/users/${userId}/profile?lang=${getApiLang()}`);
      if (!result.success) throw result;
      return result.data;
    },
    enabled: !!userId,
  });

  useTranslationCatchup(query.refetch, !query.isLoading && !!userId);

  return query;
};

export default usePublicProfile;
