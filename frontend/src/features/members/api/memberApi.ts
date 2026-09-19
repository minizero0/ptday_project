import api from '../../../lib/api';
import type { ApiResponse, PageResponse } from '../../../types/api';
import type { Member, MemberListItem, MemberSaveRequest } from '../types/member';

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

// 서버 목록 API 는 기본 정렬이 없다. 방금 등록한 회원이 맨 위에 보이도록 최근 등록순으로 요청한다.
const MEMBER_LIST_SORT = 'id,desc';

// 회원 관리 목록. 검색어가 비어 있으면 전체 회원을 페이지 단위로 내려준다.
export async function getMemberPage(
  keyword: string,
  page: number,
  size: number,
): Promise<PageResponse<MemberListItem>> {
  const { data } = await api.get<ApiResponse<PageResponse<MemberListItem>>>('/api/members', {
    // 빈 검색어는 파라미터에서 빼서 서버가 "검색 없음"으로 처리하게 한다
    params: { keyword: keyword || undefined, page, size, sort: MEMBER_LIST_SORT },
  });
  return data.data as PageResponse<MemberListItem>;
}

// 회원 등록 — 회원번호는 서버가 채번해서 응답에 담아 준다
export async function createMember(request: MemberSaveRequest): Promise<Member> {
  const { data } = await api.post<ApiResponse<Member>>('/api/members', request);
  return data.data as Member;
}

// 회원 정보 수정
export async function updateMember(id: number, request: MemberSaveRequest): Promise<Member> {
  const { data } = await api.patch<ApiResponse<Member>>(`/api/members/${id}`, request);
  return data.data as Member;
}

// 회원 삭제 (서버에서 soft delete, 관리자만 가능). 성공 시 본문 없는 204 가 온다.
export async function deleteMember(id: number): Promise<void> {
  await api.delete(`/api/members/${id}`);
}
