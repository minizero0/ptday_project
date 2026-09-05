import api from '../../../lib/api';
import type { ApiResponse } from '../../../types/api';
import type {
  AttendanceListResponse,
  CheckInRequest,
  CheckInResult,
} from '../types/attendance';

// 날짜별 출석 현황 조회. date 는 yyyy-MM-dd
export async function getDailyAttendances(date: string): Promise<AttendanceListResponse> {
  const { data } = await api.get<ApiResponse<AttendanceListResponse>>('/api/attendances', {
    params: { date },
  });
  return data.data as AttendanceListResponse;
}

// 출석 체크: 회원번호 / 연락처 뒷 4자리 / 후보 확정(memberId) 중 하나
export async function checkIn(request: CheckInRequest): Promise<CheckInResult> {
  const { data } = await api.post<ApiResponse<CheckInResult>>('/api/attendances', request);
  return data.data as CheckInResult;
}
