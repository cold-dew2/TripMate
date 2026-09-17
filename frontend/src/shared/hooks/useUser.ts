import { useQuery } from "@tanstack/react-query";
import { apiClient } from './../api/client';
import type { UserResponse } from "@/types/user";

const useUser = () => {
    return useQuery({
        queryKey: ["user"],
        queryFn: async() => {
          
          const result = await apiClient.get<UserResponse>("/trmaHome/userInfo");

          if (!result.success) {
            throw result;
          }

          return result.data.data;
        }
    });
}
export default useUser