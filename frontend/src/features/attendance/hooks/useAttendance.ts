import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { todayString } from '../../../lib/date';
import { checkIn, getDailyAttendances } from '../api/attendanceApi';
import { subscribeAttendanceEvents } from '../api/attendanceEvents';
import type { CheckInRequest } from '../types/attendance';

// 쿼리 키 컨벤션: ['attendances', date] (CLAUDE.md §6)
const attendanceKeys = {
  daily: (date: string) => ['attendances', date] as const,
};

// 새 출석은 실시간 알림(useAttendanceLiveUpdates)으로 바로 반영된다.
// 이 간격은 알림 연결이 조용히 죽었을 때를 위한 안전망이라 길게 잡는다.
const TODAY_REFRESH_INTERVAL_MS = 60_000;

// 날짜별 출석 현황 (서버 상태 — TanStack Query 로만 관리).
// 오늘을 보고 있을 때만 주기적으로 다시 불러온다 — 지난 날짜는 더 바뀌지 않는다.
export function useDailyAttendancesQuery(date: string) {
  return useQuery({
    queryKey: attendanceKeys.daily(date),
    queryFn: () => getDailyAttendances(date),
    refetchInterval: date === todayString() ? TODAY_REFRESH_INTERVAL_MS : false,
  });
}

/**
 * 출석 현황의 실시간 반영. 출석은 입구 키오스크(다른 기기)에서 찍히므로 서버가 알려줄 때 다시 불러온다.
 * 화면이 떠 있는 동안만 연결하고, 떠나면 끊는다.
 */
export function useAttendanceLiveUpdates(): void {
  const queryClient = useQueryClient();

  useEffect(
    () =>
      subscribeAttendanceEvents({
        // 연결이 끊겨 있던 동안의 변경은 알림으로 오지 않으므로, 붙을 때마다 한 번 맞춘다
        onConnected: () => queryClient.invalidateQueries({ queryKey: ['attendances'] }),
        onCheckIn: (date) => queryClient.invalidateQueries({ queryKey: attendanceKeys.daily(date) }),
      }),
    [queryClient],
  );
}

// 출석 체크. 기록이 생기면(CHECKED_IN) 해당 날짜 현황을 다시 불러온다.
// CANDIDATES(후보 선택 대기)는 아직 기록이 없으므로 invalidate 하지 않는다.
export function useCheckInMutation(date: string, isKioskScreen = false) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CheckInRequest) => checkIn(request, isKioskScreen),
    onSuccess: (result) => {
      if (result.status === 'CHECKED_IN') {
        queryClient.invalidateQueries({ queryKey: attendanceKeys.daily(date) });
      }
    },
  });
}
