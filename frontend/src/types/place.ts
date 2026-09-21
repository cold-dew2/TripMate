export interface Place {
  tourId: string;
  tourNm: string;
  sidoCd: string;
  sidoNm: string;
  sggCd: string;
  sggNm: string;
  emdCd: string;
  emdNm: string;
  roadAddr: string;
  detailAddr: string;
  zipCd: string;
  cateCd: string;
  cateNm: string;
  avgScore: number;
  firstImage?: string;
}

export interface PlaceResponse {
  success: boolean;
  status: number;
  code: string;
  message: string;
  path: string;
  token: string;
  data: Place[];
}

export interface PlaceDetail {
  avgScore: number;
  cateCd: string;
  cateNm: string;
  detailAddr: string;
  firstImage: string;
  roadAddr: string;
  sggCd: string;
  sggNm: string;
  sidoCd: string;
  sidoNm: string;
  tourId: string;
  tourNm: string;
  zipCd: string;
  overview?: string;
  latitude?: number;
  longitude?: number;
  // 좌표가 없을 때 지도가 주소로 검색하는데, roadAddr은 화면 언어로 번역돼 있어
  // 한국 주소 전용 지오코더가 인식하지 못한다. 지도 검색에는 이 원문 주소를 쓴다.
  roadAddrKo?: string;
}

export interface PlaceAIDetail {
  admissionFeeDetails: string;
  admissionFeeIsFree: string;
  closedDays: string;
  lastUpdatedNote: string;
  operatingHours: string;
  parkingAvailable: string;
  parkingFeeInfo: string;
  websiteUrl: string;
}
