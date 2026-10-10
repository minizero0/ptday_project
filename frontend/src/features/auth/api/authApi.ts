import api from '../../../lib/api';
import type { ApiResponse } from '../../../types/api';
import type { LoginRequest, LoginResponse } from '../types/auth';

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const { data } = await api.post<ApiResponse<LoginResponse>>('/api/auth/login', request);
  return data.data as LoginResponse;
}

// 입구 키오스크 계정의 토큰 연장. 현재 토큰으로 새 토큰을 받는다(본문 없음).
export async function refreshToken(): Promise<LoginResponse> {
  const { data } = await api.post<ApiResponse<LoginResponse>>('/api/auth/refresh');
  return data.data as LoginResponse;
}
