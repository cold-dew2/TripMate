import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";

// accessToken이 httpOnly 쿠키라 프론트에서 직접 로그인 여부를 읽을 수 없어,
// 가벼운 서버 확인(/login/authCheck)으로 로그인 여부를 판단한다. 로그인/로그아웃
// 시점에 react-query 캐시를 비우므로(LoginPage, MyPageScreen 로그아웃) 이 값도
// 그때 함께 갱신된다.
export const useIsLoggedIn = () => {
  const { data, isLoading } = useQuery({
    queryKey: ["authCheck"],
    queryFn: async () => {
      const r = await apiClient.get<unknown>("/login/authCheck");
      return r.success;
    },
    staleTime: 60_000,
  });

  return { isLoggedIn: data ?? false, isLoading };
};
