import api from '../../../lib/api';
import type { ApiResponse, PageResponse } from '../../../types/api';
import type { Member } from '../types/member';

// 회원 단건 조회 — 출석 그리드 행 클릭 시 상세 패널에서 사용
export async function getMember(id: number): Promise<Member> {
  const { data } = await api.get<ApiResponse<Member>>(`/api/members/${id}`);
  return data.data as Member;
}

// 회원 검색 — 이름·회원번호·전화번호를 한 번에 훑는다 (이용권 등록 시 회원 선택용)
export async function searchMembers(
  keyword: string,
  size: number,
): Promise<PageResponse<Member>> {
  const { data } = await api.get<ApiResponse<PageResponse<Member>>>('/api/members', {
    params: { keyword, page: 0, size },
  });
  return data.data as PageResponse<Member>;
}
