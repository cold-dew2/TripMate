import { useEffect } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Client } from "@stomp/stompjs";
import { apiClient } from "@/shared/api/client";
import useUser from "@/shared/hooks/useUser";

// 로그인 토큰이 httpOnly 쿠키라 JS에서 로그인 여부를 미리 알 수 없으므로,
// 항상 호출해보고 비로그인이면 서버가 NEED_LOGIN으로 응답하는 것으로 판단한다.
const useUnreadNotificationCount = () => {
  const queryClient = useQueryClient();
  const { data: user } = useUser();

  const query = useQuery({
    queryKey: ["unreadNotificationCount"],
    queryFn: async () => {
      const result = await apiClient.get<{ data: number }>("/notifications/unreadCount");
      if (!result.success) throw result;
      return result.data.data ?? 0;
    },
    // 실시간 알림(WebSocket)으로 대부분 즉시 갱신되므로, 폴링은 연결이 끊겼을 때를
    // 대비한 보조 수단으로만 더 긴 주기로 둔다.
    refetchInterval: 60000,
  });

  // 새 알림(모임 신청/채팅)이 생기면 서버가 /topic/notifications/{userId}로 신호를 보내고,
  // 그걸 받으면 알림/채팅 안읽음 개수를 즉시 다시 불러온다.
  useEffect(() => {
    if (!user?.userId) return;

    const api = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
    const wsUrl = api.replace(/^http/, "ws") + "/ws/chat-native";
    const stomp = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 3000,
      onConnect: () => {
        stomp.subscribe(`/topic/notifications/${user.userId}`, () => {
          queryClient.invalidateQueries({ queryKey: ["unreadNotificationCount"] });
          queryClient.invalidateQueries({ queryKey: ["chatUnreadCount"] });
          queryClient.invalidateQueries({ queryKey: ["notifications"] });
        });
      },
    });
    stomp.activate();

    return () => {
      void stomp.deactivate();
    };
  }, [user?.userId, queryClient]);

  return query;
};

export default useUnreadNotificationCount;
