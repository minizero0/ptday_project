import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { QueryClient } from '@tanstack/react-query';
import {
  createMember,
  deleteMember,
  getMember,
  getMemberPage,
  searchMembers,
  updateMember,
} from '../api/memberApi';
import type { MemberSaveRequest } from '../types/member';

// 쿼리 키 컨벤션: ['members', memberId] (CLAUDE.md §6)
const memberKeys = {
  detail: (id: number) => ['members', id] as const,
  search: (keyword: string) => ['members', 'search', keyword] as const,
  list: (keyword: string, page: number) => ['members', 'list', keyword, page] as const,
};

// 회원 관리 목록 한 페이지 크기
export const MEMBER_PAGE_SIZE = 20;

// 출석 현황·이용권 목록은 회원 이름/번호를 함께 담아 내려온다.
// 회원 정보가 바뀌거나 삭제되면 그 화면들의 캐시도 낡으므로 같이 무효화한다.
const MEMBER_DEPENDENT_QUERY_ROOTS = ['members', 'memberships', 'attendances'] as const;

function invalidateMemberDependents(queryClient: QueryClient) {
  MEMBER_DEPENDENT_QUERY_ROOTS.forEach((root) => {
    queryClient.invalidateQueries({ queryKey: [root] });
  });
}

// 검색 결과로 한 번에 보여줄 최대 건수 (더 필요하면 검색어를 좁힌다)
const MEMBER_SEARCH_SIZE = 8;

// 회원 단건 조회. id 가 null 이면(선택된 행 없음) 요청하지 않는다.
export function useMemberQuery(id: number | null) {
  return useQuery({
    queryKey: memberKeys.detail(id ?? -1),
    queryFn: () => getMember(id as number),
    enabled: id !== null,
  });
}

// 회원 검색. 검색어가 비어 있으면 요청하지 않는다.
export function useMemberSearchQuery(keyword: string) {
  const trimmed = keyword.trim();

  return useQuery({
    queryKey: memberKeys.search(trimmed),
    queryFn: () => searchMembers(trimmed, MEMBER_SEARCH_SIZE),
    enabled: trimmed.length > 0,
  });
}

// 회원 관리 목록. 페이지를 넘길 때 목록이 빈 화면으로 깜빡이지 않도록 이전 데이터를 유지한다.
export function useMemberPageQuery(keyword: string, page: number) {
  const trimmed = keyword.trim();

  return useQuery({
    queryKey: memberKeys.list(trimmed, page),
    queryFn: () => getMemberPage(trimmed, page, MEMBER_PAGE_SIZE),
    placeholderData: (previous) => previous,
  });
}

// 회원 등록. 새 회원은 다른 화면에 아직 등장하지 않으므로 회원 쿼리만 무효화한다.
export function useCreateMemberMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: MemberSaveRequest) => createMember(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['members'] });
    },
  });
}

// 회원 정보 수정
export function useUpdateMemberMutation(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: MemberSaveRequest) => updateMember(id, request),
    onSuccess: () => invalidateMemberDependents(queryClient),
  });
}

// 회원 삭제 (관리자 전용 — 권한이 없으면 서버가 403 을 준다)
export function useDeleteMemberMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => deleteMember(id),
    onSuccess: () => invalidateMemberDependents(queryClient),
  });
}
