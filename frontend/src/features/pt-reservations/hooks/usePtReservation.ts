import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { QueryClient } from '@tanstack/react-query';
import {
  cancelPtReservation,
  createPtReservation,
  getPtReservations,
} from '../api/ptReservationApi';
import type { PtReservationCreateRequest, PtReservationQuery } from '../types/ptReservation';

// 쿼리 키 컨벤션: ['pt-reservations', { trainerId, from, to }] (CLAUDE.md §6)
const ptReservationKeys = {
  list: (query: PtReservationQuery) => ['pt-reservations', query] as const,
};

// 예약·취소는 PT 잔여 횟수를 바꾼다 → PT권 목록·변동 이력과 회원 목록의 "PT 잔여"도 낡는다.
const PT_RESERVATION_DEPENDENT_QUERY_ROOTS = ['pt-reservations', 'pt-passes', 'members'] as const;

function invalidatePtReservationDependents(queryClient: QueryClient) {
  PT_RESERVATION_DEPENDENT_QUERY_ROOTS.forEach((root) => {
    queryClient.invalidateQueries({ queryKey: [root] });
  });
}

const NO_QUERY: PtReservationQuery = { trainerId: -1, from: '', to: '' };

// 트레이너가 아직 선택되지 않았으면(null) 요청하지 않는다.
// 주를 넘길 때 시간표가 통째로 깜빡이지 않게 이전 주 데이터를 잠시 유지한다.
// 단, 트레이너를 바꿨을 때는 유지하지 않는다 — 다른 트레이너의 예약이 새 트레이너 것처럼 보이면 안 된다.
export function usePtReservationsQuery(query: PtReservationQuery | null) {
  const activeQuery = query ?? NO_QUERY;

  return useQuery({
    queryKey: ptReservationKeys.list(activeQuery),
    queryFn: () => getPtReservations(activeQuery),
    enabled: query !== null,
    placeholderData: (previousData, previousQuery) => {
      const previousTrainerId = (previousQuery?.queryKey[1] as PtReservationQuery | undefined)
        ?.trainerId;
      return previousTrainerId === activeQuery.trainerId ? previousData : undefined;
    },
  });
}

export function useCreatePtReservationMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: PtReservationCreateRequest) => createPtReservation(request),
    onSuccess: () => invalidatePtReservationDependents(queryClient),
  });
}

export function useCancelPtReservationMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (reservationId: number) => cancelPtReservation(reservationId),
    onSuccess: () => invalidatePtReservationDependents(queryClient),
  });
}
