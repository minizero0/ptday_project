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

// 백엔드 PageResponse 와 1:1 대응 (common/response/PageResponse)
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
