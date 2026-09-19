// 백엔드 MembershipResponse 와 1:1 대응 (domain/membership/dto/MembershipResponse)
export interface Membership {
  id: number;
  memberId: number;
  startDate: string; // yyyy-MM-dd
  endDate: string; // yyyy-MM-dd
  status: MembershipStatus;
  // "이용중" 여부와 남은 일수는 서버가 오늘 기준으로 판단해 내려준다 — 화면에서 다시 계산하지 않는다 (CLAUDE.md §6)
  active: boolean;
  daysRemaining: number | null; // 이용중이 아니면 null, 만료일 당일이면 0
  createdAt: string; // ISO-8601 UTC — 표시할 때만 로컬 변환 (CLAUDE.md §5)
}

export type MembershipStatus = 'ACTIVE' | 'EXPIRED';

// 기준일에서 본 기간 상태. 서버(MembershipPeriodStatus)가 판단해 내려준다.
export type MembershipPeriodStatus = 'SCHEDULED' | 'ACTIVE' | 'EXPIRED';

// 이용권 관리 목록의 한 줄 (MembershipListItemResponse). 어느 회원 것인지 함께 담는다.
export interface MembershipListItem {
  id: number;
  memberId: number;
  memberNo: string;
  memberName: string;
  startDate: string; // yyyy-MM-dd
  endDate: string; // yyyy-MM-dd
  status: MembershipPeriodStatus;
  daysRemaining: number | null; // 이용중이 아니면 null
}

// 이용권 수정 모달이 필요로 하는 값. 이용권 관리 목록(MembershipListItem)에서도,
// 회원 상세의 이력(Membership + 회원 정보)에서도 열 수 있게 공통 부분만 요구한다.
export interface MembershipEditTarget {
  id: number;
  memberNo: string;
  memberName: string;
  startDate: string; // yyyy-MM-dd
  endDate: string; // yyyy-MM-dd
}

// 회원 한 줄에 붙는 이용권 요약 (MembershipSummaryResponse). 회원 목록이 대표 이용권 1건을 이 모양으로 내려준다.
export interface RepresentativeMembership {
  id: number;
  status: MembershipPeriodStatus;
  startDate: string; // yyyy-MM-dd
  endDate: string; // yyyy-MM-dd
  daysRemaining: number | null; // 이용중이 아니면 null, 만료일 당일이면 0
}

// 이용권 부여·수정 요청. 기간의 진실은 시작일·만료일 두 값뿐이다.
// 개월 수는 만료일을 채우는 화면 계산 수단이라 서버로 보내지 않는다.
export interface MembershipPeriodRequest {
  startDate: string; // yyyy-MM-dd
  endDate: string; // yyyy-MM-dd
}

// 만료일 빠른 계산 버튼. 서버 제약이 아니라 화면 편의 값이다.
export const MEMBERSHIP_PLAN_MONTHS = [1, 3, 6, 12] as const;
