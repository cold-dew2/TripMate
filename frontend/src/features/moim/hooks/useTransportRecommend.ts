import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import type { PlanItem } from "../pages/moimCreate/MoimCreate";

export interface TransportStopInput {
  day: number;
  time: string;
  tourId: string;
  tourNm: string;
  roadAddr?: string;
}

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
  // 실시간 교통 API 연동 없이 AI가 주소/시각을 바탕으로 추정한 혼잡도 예측이다.
  congestionLevel?: "원활" | "보통" | "혼잡";
  delayRiskMinutes?: number;
  alternativeMode?: string;
  alternativeReason?: string;
}

// 소모임 생성(Step5)의 dayIndex 기반 itemsByDay를 그대로 서버 입력 형태로 변환한다.
export const toTransportStops = (itemsByDay: Record<number, PlanItem[]>): TransportStopInput[] =>
  Object.entries(itemsByDay).flatMap(([day, dayItems]) =>
    dayItems.map((item) => ({
      day: Number(day),
      time: item.time,
      tourId: item.tourId,
      tourNm: item.placeName,
      roadAddr: item.roadAddr,
    }))
  );

export const useTransportRecommend = () => {
  return useMutation({
    mutationFn: async (items: TransportStopInput[]) => {
      const result = await apiClient.post<{ data: TransportLeg[] }>("/tourList/transportRecommend", { items });
      if (!result.success) throw result;
      return result.data.data ?? [];
    },
  });
};

export default useTransportRecommend;
