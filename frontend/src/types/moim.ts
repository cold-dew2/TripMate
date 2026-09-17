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
  courseStops?: { name: string; address: string; sidoNm?: string; sggNm?: string }[];
}

export interface MoimCreateForm {
  moimTitle: string;
  moimDscr: string;
  moimStartDt: string;
  moimEndDt: string;
  maxMember: number;
  moimImgUrl?: string;
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
  reviewScore: number;
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
  tourNm: string;
}

export interface MoimJoinStatus {
  roleCd: string;
  stateCd: string;
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
}

