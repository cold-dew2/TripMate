export interface TourReview {
  moimTitle: string;
  moimStartDt: string;
  moimEndDt: string;
  reviewTitle: string;
  reviewContent: string;
  reviewScore: number;
  imgUrls: string | null;
  creatDt: string;
  userNm: string;
}

export interface TourReviewResponse {
  success: boolean;
  status: number;
  code: string;
  message: string;
  path: string;
  token: string | null;
  data: TourReview[];
}
