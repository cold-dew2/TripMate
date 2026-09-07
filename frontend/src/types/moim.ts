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

export interface MoimCreateForm {
  moimTitle: string;
  moimDscr: string;
  moimStartDt: string;
  moimEndDt: string;
  maxMember: number;
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