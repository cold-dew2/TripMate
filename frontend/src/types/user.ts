export interface User {
  userNm: string;
  langCd: string;
  langNm: string;
  stateCd: string;
  stateNm: string;
  roleCd: string;
  roleNm: string;
  birthDt: string;
  genderCd: string;
  genderNm: string;
  phoneNum: string;
}

export interface UserResponse {
  success: boolean;
  status: number;
  code: string;
  message: string;
  path: string;
  token: string;
  data: User;
}