import type { SessionMinutes } from '../../pt-passes/types/ptPass';

export type PtReservationStatus = 'RESERVED' | 'CANCELED';

// 백엔드 PtReservationResponse 와 1:1 대응. 시간표 한 칸을 그리는 데 필요한 값이 모두 들어 있다.
export interface PtReservation {
  id: number;
  memberId: number;
  memberNo: string;
  memberName: string;
  trainerId: number;
  trainerName: string;
  ptPassId: number; // 1회가 차감된 PT권
  sessionMinutes: SessionMinutes;
  startTime: string; // ISO-8601 UTC — 표시할 때만 로컬 변환 (CLAUDE.md §5)
  endTime: string; // 서버가 시작 + 수업 길이로 계산한다
  status: PtReservationStatus;
  createdBy: string;
  createdAt: string;
  canceledAt: string | null;
}

// 어느 PT권에서 차감할지는 보내지 않는다 — 수업 길이만 고르면 서버가 먼저 산 PT권부터 쓴다.
export interface PtReservationCreateRequest {
  memberId: number;
  trainerId: number;
  sessionMinutes: SessionMinutes;
  startTime: string; // ISO-8601 UTC
}

// 시간표 조회 범위. 날짜는 yyyy-MM-dd 이고 둘 다 포함한다.
export interface PtReservationQuery {
  trainerId: number;
  from: string;
  to: string;
}
