import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiLang } from "@/shared/utils/lang";

export interface MyTodayScheduleItem {
  time: string;
  placeName: string;
}

export interface MyTodayScheduleMoim {
  moimId: string;
  moimTitle: string;
  items: MyTodayScheduleItem[];
}

// 오늘 진행 중인(가입 승인된) 모임이 동시에 여러 개일 수 있어 배열로 받는다.
export const useMyTodaySchedule = (enabled = true) => {
  return useQuery({
    queryKey: ["myTodaySchedule", getApiLang()],
    queryFn: async () => {
      const result = await apiClient.get<{ data: MyTodayScheduleMoim[] }>("/moimList/myTodaySchedule", { lang: getApiLang() });
      if (!result.success) throw result;
      return result.data.data ?? [];
    },
    enabled,
  });
};

export default useMyTodaySchedule;
