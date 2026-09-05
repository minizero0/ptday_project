import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { checkIn, getDailyAttendances } from '../api/attendanceApi';
import type { CheckInRequest } from '../types/attendance';

// 쿼리 키 컨벤션: ['attendances', date] (CLAUDE.md §6)
const attendanceKeys = {
  daily: (date: string) => ['attendances', date] as const,
};

// 날짜별 출석 현황 (서버 상태 — TanStack Query 로만 관리)
export function useDailyAttendancesQuery(date: string) {
  return useQuery({
    queryKey: attendanceKeys.daily(date),
    queryFn: () => getDailyAttendances(date),
  });
}

// 출석 체크. 기록이 생기면(CHECKED_IN) 해당 날짜 현황을 다시 불러온다.
// CANDIDATES(후보 선택 대기)는 아직 기록이 없으므로 invalidate 하지 않는다.
export function useCheckInMutation(date: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CheckInRequest) => checkIn(request),
    onSuccess: (result) => {
      if (result.status === 'CHECKED_IN') {
        queryClient.invalidateQueries({ queryKey: attendanceKeys.daily(date) });
      }
    },
  });
}
