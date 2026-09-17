import { useQuery } from "@tanstack/react-query";
import { apiClient } from './../api/client';
import type { UserResponse } from "@/types/user";

const useUser = () => {
    const isLoggedIn = !!(localStorage.getItem("accessToken") || sessionStorage.getItem("accessToken"));

    return useQuery({
        queryKey: ["user"],
        queryFn: async() => {

          const result = await apiClient.get<UserResponse>("/trmaHome/userInfo");

          if (!result.success) {
            throw result;
          }

          return result.data.data;
        },
        enabled: isLoggedIn,
    });
}
export default useUser