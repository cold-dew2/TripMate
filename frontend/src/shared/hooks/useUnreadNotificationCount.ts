import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";

const useUnreadNotificationCount = () => {
  const isLoggedIn = !!(localStorage.getItem("accessToken") || sessionStorage.getItem("accessToken"));

  return useQuery({
    queryKey: ["unreadNotificationCount"],
    queryFn: async () => {
      const result = await apiClient.get<{ data: number }>("/notifications/unreadCount");
      if (!result.success) throw result;
      return result.data.data ?? 0;
    },
    enabled: isLoggedIn,
    refetchInterval: 20000,
  });
};

export default useUnreadNotificationCount;
