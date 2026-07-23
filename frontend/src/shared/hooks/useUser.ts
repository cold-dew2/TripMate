import { useQuery } from "@tanstack/react-query";
import { apiClient } from './../api/client';

interface User {
    name: string;
}

const useUser = () => {
    return useQuery({
        queryKey: ["user"],
        queryFn: async() => {
          
          const result = await apiClient.get<User>("/auth/me.json");

          if (!result.success) {
            throw result;
          }

          return result.data;
        }
    });
}
export default useUser