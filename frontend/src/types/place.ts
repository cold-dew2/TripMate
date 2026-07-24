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