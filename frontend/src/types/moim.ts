export interface Moim {
  moimId: string;
  moimTitle: string;      
  moimDscr: string;     
  moimStartDt: string;    
  moimEndDt: string;      
  userId: string;         
  userNm: string;         
  cateCd: string;       
  cateNm: string;         
  memberCnt: string;      
  visitCnt: number;
  imageUrl: string;
  region: string;
  userRating: string;
  maxMember?: number;
  avgScore?: number;
}

export interface MoimResponse {
  success: true,
  status: 0,
  code: string,
  message: string,
  path: string,
  token: string,
  data: Moim[]
}

export interface MoimCreatePrefill {
  title?: string;
  dscr?: string;
  themeId?: string;
  region?: string;
  courseStops?: { name: string; address: string; sidoNm?: string; sggNm?: string }[];
}

export interface MoimCreateForm {
  moimTitle: string;
  moimDscr: string;
  moimStartDt: string;
  moimEndDt: string;
  maxMember: number;
  moimImgUrl?: string;
  region?: string;
  dayCount?: number;
  moimCateData: {
    cateCd: string;
    cateNm?: string;
  }[];
  moimPlanData: {
    startDt: string;
    rmks: string;
    tourId: string;
  }[];
}

export interface MoimDetail {
  moimId: string;
  moimTitle: string;
  moimDscr: string;
  moimStartDt: string;
  moimEndDt: string;
  maxMember: number;
  memberCnt: number;
  userId: string;
  userNm: string;
  reviewScore: number | null;
  imageUrl?: string;
}

export interface MoimCategory {
  cateCd: string;
  cateNm: string;
}

export interface MoimPlan {
  startDt: string;
  rmks: string;
  cateCd: string;
  cateNm: string;
  tourId: string;
  tourNm: string;
  roadAddr: string;
}

export interface MoimJoinStatus {
  roleCd: string;
  stateCd: string;
}

export interface MoimReviewStatus {
  moimReviewedYn: string;
  tourReviewedYn: string;
}

export interface MyMoim {
  moimId: string;
  moimTitle: string;
  moimDscr: string;
  moimStartDt: string;
  moimEndDt: string;
  maxMember: number;
  memberCnt: number;
  roleCd: string;
  stateCd: string;
  cateCd: string;
  cateNm: string;
  reviewedYn: string;
}

export interface MoimMember {
  userId: string;
  userNm: string;
  roleCd: string;
  stateCd: string;
}

export interface MoimDetailResponse {
  success: boolean;
  status: number;
  code: string;
  message: string;
  path: string;
  token: string | null;
  data: MoimDetail;
  cate: MoimCategory[];
  plan: MoimPlan[];
  joinStatus: MoimJoinStatus | null;
  reviewStatus: MoimReviewStatus | null;
}

