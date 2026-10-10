import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { QueryClient } from '@tanstack/react-query';
import {
  adjustPtPass,
  getPtPassAdjustments,
  getPtPasses,
  getPtPassPage,
  grantPtPass,
} from '../api/ptPassApi';
import type {
  PtPassAdjustRequest,
  PtPassCreateRequest,
  PtPassSearchFilter,
} from '../types/ptPass';

// 쿼리 키 컨벤션: ['pt-passes', memberId] (CLAUDE.md §6)
const ptPassKeys = {
  byMember: (memberId: number) => ['pt-passes', memberId] as const,
  adjustments: (ptPassId: number) => ['pt-passes', 'adjustments', ptPassId] as const,
  list: (filter: PtPassSearchFilter, page: number) => ['pt-passes', 'list', filter, page] as const,
};

// PT권 관리 목록 한 페이지 크기
export const PT_PASS_PAGE_SIZE = 20;

// 회원 목록은 회원별 PT 잔여 합계를 함께 담아 내려온다.
// PT권을 부여·조정하면 그 숫자도 낡으므로 같이 무효화한다.
const PT_PASS_DEPENDENT_QUERY_ROOTS = ['pt-passes', 'members'] as const;

function invalidatePtPassDependents(queryClient: QueryClient) {
  PT_PASS_DEPENDENT_QUERY_ROOTS.forEach((root) => {
    queryClient.invalidateQueries({ queryKey: [root] });
  });
}

// 회원의 PT권 목록. memberId 가 null 이면(선택된 회원 없음) 요청하지 않는다.
export function usePtPassesQuery(memberId: number | null) {
  return useQuery({
    queryKey: ptPassKeys.byMember(memberId ?? -1),
    queryFn: () => getPtPasses(memberId as number),
    enabled: memberId !== null,
  });
}

// PT권 관리 목록. 페이지를 넘길 때 목록이 빈 화면으로 깜빡이지 않도록 이전 데이터를 유지한다.
export function usePtPassPageQuery(filter: PtPassSearchFilter, page: number) {
  return useQuery({
    queryKey: ptPassKeys.list(filter, page),
    queryFn: () => getPtPassPage(filter, page, PT_PASS_PAGE_SIZE),
    placeholderData: (previous) => previous,
  });
}

// PT권 한 개의 횟수 변동 이력
export function usePtPassAdjustmentsQuery(ptPassId: number) {
  return useQuery({
    queryKey: ptPassKeys.adjustments(ptPassId),
    queryFn: () => getPtPassAdjustments(ptPassId),
  });
}

export function useGrantPtPassMutation(memberId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: PtPassCreateRequest) => grantPtPass(memberId, request),
    onSuccess: () => invalidatePtPassDependents(queryClient),
  });
}

export function useAdjustPtPassMutation(ptPassId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: PtPassAdjustRequest) => adjustPtPass(ptPassId, request),
    onSuccess: () => invalidatePtPassDependents(queryClient),
  });
}
