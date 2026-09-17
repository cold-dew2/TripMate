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
