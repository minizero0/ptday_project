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
// 입구 출석 키오스크 전용 계정. 출석 화면(/kiosk)만 쓸 수 있다.
export const ROLE_KIOSK = 'KIOSK';
