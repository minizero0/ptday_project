export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  username: string;
  role: string;
}

// 백엔드 Role enum 과 같은 값. 회원 삭제처럼 관리자 전용 기능의 화면 제어에 쓴다.
export const ROLE_ADMIN = 'ADMIN';
