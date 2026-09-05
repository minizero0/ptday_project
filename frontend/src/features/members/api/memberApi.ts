import api from '../../../lib/api';
import type { ApiResponse } from '../../../types/api';
import type { Member } from '../types/member';

// 회원 단건 조회 — 출석 그리드 행 클릭 시 상세 패널에서 사용
export async function getMember(id: number): Promise<Member> {
  const { data } = await api.get<ApiResponse<Member>>(`/api/members/${id}`);
  return data.data as Member;
}
