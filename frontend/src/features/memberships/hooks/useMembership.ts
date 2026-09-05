import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getMembershipPage,
  getMemberships,
  grantMembership,
  updateMembership,
} from '../api/membershipApi';
import type { MembershipPeriodRequest } from '../types/membership';

// 쿼리 키 컨벤션: ['memberships', memberId] (CLAUDE.md §6)
const membershipKeys = {
  byMember: (memberId: number) => ['memberships', memberId] as const,
  list: (includeExpired: boolean, page: number) =>
    ['memberships', 'list', includeExpired, page] as const,
};

// 이용권 관리 목록 한 페이지 크기
export const MEMBERSHIP_PAGE_SIZE = 20;

// 회원의 이용권 이력. memberId 가 null 이면(선택된 회원 없음) 요청하지 않는다.
export function useMembershipsQuery(memberId: number | null) {
  return useQuery({
    queryKey: membershipKeys.byMember(memberId ?? -1),
    queryFn: () => getMemberships(memberId as number),
    enabled: memberId !== null,
  });
}

// 이용권 부여. 성공하면 해당 회원의 이력을 다시 불러와 (이용중) 배지까지 갱신한다.
export function useGrantMembershipMutation(memberId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: MembershipPeriodRequest) => grantMembership(memberId, request),
    onSuccess: () => {
      // 회원별 이력과 관리 목록이 모두 바뀌므로 이용권 쿼리 전체를 무효화한다
      queryClient.invalidateQueries({ queryKey: ['memberships'] });
    },
  });
}

// 이용권 관리 목록. 페이지를 넘길 때 목록이 빈 화면으로 깜빡이지 않도록 이전 데이터를 유지한다.
export function useMembershipPageQuery(includeExpired: boolean, page: number) {
  return useQuery({
    queryKey: membershipKeys.list(includeExpired, page),
    queryFn: () => getMembershipPage(includeExpired, page, MEMBERSHIP_PAGE_SIZE),
    placeholderData: (previous) => previous,
  });
}

// 이용권 기간 정정. 성공하면 목록과 회원별 이력을 모두 다시 불러온다.
export function useUpdateMembershipMutation(membershipId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: MembershipPeriodRequest) => updateMembership(membershipId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['memberships'] });
    },
  });
}
