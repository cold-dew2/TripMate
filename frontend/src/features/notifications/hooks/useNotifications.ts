import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";

// .env의 VITE_API_BASE_URL이 /data(로컬 목업 JSON)를 가리킬 때는
// /notifications와 /notifications/unreadCount가 정적 파일 경로상 충돌하므로
// 목록은 /notifications/list 파일로 분리해 요청한다.
const isMock = (import.meta.env.VITE_API_BASE_URL ?? "").startsWith("/data");

export interface Notification {
  notiId: number;
  typeCd: "APPLY" | "CHAT" | "COMPLETE";
  title: string;
  content: string;
  linkUrl: string;
  isRead: "Y" | "N";
  createDt: string;
}

export const useNotifications = () => {
  return useQuery({
    queryKey: ["notifications"],
    queryFn: async () => {
      const result = await apiClient.get<{ data: Notification[] }>(isMock ? "/notifications/list" : "/notifications");
      if (!result.success) throw result;
      return result.data.data ?? [];
    },
    refetchInterval: 20000,
  });
};

export const useMarkNotificationRead = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (notiId: number) => {
      const result = await apiClient.put(`/notifications/${notiId}/read`, {});
      if (!result.success) throw result;
      return result.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
      queryClient.invalidateQueries({ queryKey: ["unreadNotificationCount"] });
    },
  });
};

export default useNotifications;
