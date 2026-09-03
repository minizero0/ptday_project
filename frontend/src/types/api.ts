// 백엔드 공통 응답 래퍼 (CLAUDE.md §5)
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: ApiError;
}

export interface ApiError {
  code: string;
  message: string;
}
