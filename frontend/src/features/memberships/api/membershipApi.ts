import api from '../../../lib/api';
import type { ApiResponse, PageResponse } from '../../../types/api';
import type { Membership, MembershipListItem, MembershipPeriodRequest } from '../types/membership';

// 회원별 이용권 이력 — 최근 시작일 순으로 내려온다
export async function getMemberships(memberId: number): Promise<Membership[]> {
  const { data } = await api.get<ApiResponse<Membership[]>>(
    `/api/members/${memberId}/memberships`,
  );
  return data.data as Membership[];
}

// 이용권 부여
export async function grantMembership(
  memberId: number,
  request: MembershipPeriodRequest,
): Promise<Membership> {
  const { data } = await api.post<ApiResponse<Membership>>(
    `/api/members/${memberId}/memberships`,
    request,
  );
  return data.data as Membership;
}

// 이용권 관리 목록. 기본은 아직 끝나지 않은 이용권만 만료 임박순으로 내려온다.
export async function getMembershipPage(
  includeExpired: boolean,
  page: number,
  size: number,
): Promise<PageResponse<MembershipListItem>> {
  const { data } = await api.get<ApiResponse<PageResponse<MembershipListItem>>>(
    '/api/memberships',
    { params: { includeExpired, page, size } },
  );
  return data.data as PageResponse<MembershipListItem>;
}

// 이용권 기간 정정 (시작일·만료일)
export async function updateMembership(
  membershipId: number,
  request: MembershipPeriodRequest,
): Promise<Membership> {
  const { data } = await api.patch<ApiResponse<Membership>>(
    `/api/memberships/${membershipId}`,
    request,
  );
  return data.data as Membership;
}
