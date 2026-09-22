import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiLang } from "@/shared/utils/lang";
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

export interface TransportRecommendResult {
  legs: TransportLeg[];
  // legs가 비어있는 이유를 구분하기 위한 코드. "같은 날 2곳 이상 없어서 애초에
  // 분석할 구간이 없었다"와 "구간은 있었는데 AI가 일시적으로 응답하지 못했다"를
  // 같은 빈 배열로 뭉뚱그리면 사용자에게 안내할 문구를 고를 수 없어서 넘겨받는다.
  code?: string;
}

export const useTransportRecommend = () => {
  return useMutation({
    mutationFn: async (items: TransportStopInput[]): Promise<TransportRecommendResult> => {
      const result = await apiClient.post<{ data: TransportLeg[]; code?: string }>("/tourList/transportRecommend", { items, lang: getApiLang() });
      if (!result.success) throw result;
      return { legs: result.data.data ?? [], code: result.data.code };
    },
  });
};

export default useTransportRecommend;
