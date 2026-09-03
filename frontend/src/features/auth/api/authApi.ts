import api from '../../../lib/api';
import type { ApiResponse } from '../../../types/api';
import type { LoginRequest, LoginResponse } from '../types/auth';

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const { data } = await api.post<ApiResponse<LoginResponse>>('/api/auth/login', request);
  return data.data as LoginResponse;
}
