import axios from 'axios';
import type { ApiResponse } from '../types/api';

// 서버가 내려준 친절한 메시지(error.message)를 우선 보여주고, 없으면 화면별 기본 문구를 쓴다 (CLAUDE.md §7.2)
export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiResponse<never>>(error)) {
    return error.response?.data?.error?.message ?? fallback;
  }
  return fallback;
}
