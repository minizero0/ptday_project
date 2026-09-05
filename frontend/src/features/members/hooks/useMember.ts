import { useQuery } from '@tanstack/react-query';
import { getMember } from '../api/memberApi';

// 쿼리 키 컨벤션: ['members', memberId] (CLAUDE.md §6)
const memberKeys = {
  detail: (id: number) => ['members', id] as const,
};

// 회원 단건 조회. id 가 null 이면(선택된 행 없음) 요청하지 않는다.
export function useMemberQuery(id: number | null) {
  return useQuery({
    queryKey: memberKeys.detail(id ?? -1),
    queryFn: () => getMember(id as number),
    enabled: id !== null,
  });
}
