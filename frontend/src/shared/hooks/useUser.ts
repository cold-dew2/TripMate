import { useQuery } from "@tanstack/react-query";
import { apiClient } from './../api/client';
import type { UserResponse } from "@/types/user";

// 로그인 토큰이 httpOnly 쿠키라 JS에서 로그인 여부를 미리 알 수 없으므로,
// 항상 호출해보고 비로그인이면 서버가 NEED_LOGIN으로 응답하는 것으로 판단한다.
const useUser = () => {
    return useQuery({
        queryKey: ["user"],
        queryFn: async() => {

          const result = await apiClient.get<UserResponse>("/trmaHome/userInfo");

          if (!result.success) {
            throw result;
          }

          return result.data.data;
        },
        // 로그인 여부는 앱 전역 staleTime 기본값(60초)을 따르지 않고 항상 새로 확인한다
        // (세션 만료/로그아웃 직후에도 잠깐 "로그인된 것처럼" 보이면 안 되므로).
        staleTime: 0,
    });
}
export default useUser