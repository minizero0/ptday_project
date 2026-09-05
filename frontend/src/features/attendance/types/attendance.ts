import type { MembershipPeriodStatus } from '../../memberships/types/membership';

// 백엔드 attendance DTO 와 1:1 대응 (domain/attendance/dto)

export interface Attendance {
  id: number;
  memberId: number;
  memberNo: string;
  name: string;
  checkedInAt: string; // ISO-8601 UTC — 표시할 때만 로컬 변환 (CLAUDE.md §5)
  membership: AttendanceMembership | null; // 이용권 이력이 없으면 null
}

// 그리드 한 줄에 표시할 이용권 요약 (AttendanceResponse.MembershipSummary)
export interface AttendanceMembership {
  status: MembershipPeriodStatus;
  startDate: string; // yyyy-MM-dd
  endDate: string; // yyyy-MM-dd
}

export interface AttendanceListResponse {
  date: string; // yyyy-MM-dd
  totalCount: number;
  items: Attendance[];
}

// 출석 체크 요청: 세 키 중 정확히 하나만 담는다
export interface CheckInRequest {
  memberNo?: string;
  phoneLastDigits?: string;
  memberId?: number;
}

export type CheckInStatus = 'CHECKED_IN' | 'CANDIDATES';

// 연락처 뒷번호 중복 시 직원이 고를 후보
export interface CheckInCandidate {
  memberId: number;
  memberNo: string;
  name: string;
  maskedPhone: string | null;
}

export interface CheckInResult {
  status: CheckInStatus;
  attendance: Attendance | null;
  candidates: CheckInCandidate[] | null;
}
