import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import type { PlanItem } from "../pages/moimCreate/MoimCreate";

export interface TransportLeg {
  day: number;
  fromTourId: string;
  fromTourNm: string;
  toTourId: string;
  toTourNm: string;
  mode: string;
  durationMinutes?: number;
  cost?: number;
  transferCount?: number;
}

export const useTransportRecommend = () => {
  return useMutation({
    mutationFn: async (itemsByDay: Record<number, PlanItem[]>) => {
      const items = Object.entries(itemsByDay).flatMap(([day, dayItems]) =>
        dayItems.map((item) => ({
          day: Number(day),
          time: item.time,
          tourId: item.tourId,
          tourNm: item.placeName,
          roadAddr: item.roadAddr,
        }))
      );

      const result = await apiClient.post<{ data: TransportLeg[] }>("/tourList/transportRecommend", { items });
      if (!result.success) throw result;
      return result.data.data ?? [];
    },
  });
};

export default useTransportRecommend;
