import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import type { MyMoim } from "@/types/moim";

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
  return useQuery({
    queryKey: ["publicProfile", userId],
    queryFn: async () => {
      const result = await apiClient.get<PublicProfileResult>(`/users/${userId}/profile`);
      if (!result.success) throw result;
      return result.data;
    },
    enabled: !!userId,
  });
};

export default usePublicProfile;
