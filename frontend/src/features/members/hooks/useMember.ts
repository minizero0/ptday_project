import { useQuery } from '@tanstack/react-query';
import { getMember, searchMembers } from '../api/memberApi';

// 쿼리 키 컨벤션: ['members', memberId] (CLAUDE.md §6)
const memberKeys = {
  detail: (id: number) => ['members', id] as const,
  search: (keyword: string) => ['members', 'search', keyword] as const,
};

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
