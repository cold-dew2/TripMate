import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";

// 로그인 토큰이 httpOnly 쿠키라 JS에서 로그인 여부를 미리 알 수 없으므로,
// 항상 호출해보고 비로그인이면 서버가 NEED_LOGIN으로 응답하는 것으로 판단한다.
// 새 채팅이 오면 useUnreadNotificationCount의 WebSocket 구독이 이 쿼리도 함께 invalidate한다.
const useChatUnreadCount = () => {
  return useQuery({
    queryKey: ["chatUnreadCount"],
    queryFn: async () => {
      const result = await apiClient.get<{ data: number }>("/chat/unreadCount");
      if (!result.success) throw result;
      return result.data.data ?? 0;
    },
    refetchInterval: 60000,
  });
};

export default useChatUnreadCount;
