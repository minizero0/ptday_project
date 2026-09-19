// 백엔드 PtPassResponse 와 1:1 대응 (domain/ptpass/dto/PtPassResponse)
export interface PtPass {
  id: number;
  memberId: number;
  sessionMinutes: SessionMinutes; // 1회 수업 길이. 부여 후 바뀌지 않고, 예약 길이를 결정한다
  totalCount: number; // 구매(부여) 횟수. 부여 후 바뀌지 않는다
  remainingCount: number; // 예약 차감·수동 조정으로만 움직인다
  createdAt: string; // ISO-8601 UTC — 표시할 때만 로컬 변환 (CLAUDE.md §5)
}

// 판매하는 수업 길이(분). 서버(PtPass.ALLOWED_SESSION_MINUTES)와 같은 값이다.
export const SESSION_MINUTES_OPTIONS = [30, 40, 50, 60] as const;
export type SessionMinutes = (typeof SESSION_MINUTES_OPTIONS)[number];

// 잔여 횟수가 바뀐 경로: 직원의 수동 조정 / 예약 시 자동 차감 / 예약 취소 시 자동 복원
export type PtPassAdjustmentType = 'MANUAL' | 'RESERVATION' | 'RESERVATION_CANCEL';

export const ADJUSTMENT_TYPE_LABELS: Record<PtPassAdjustmentType, string> = {
  MANUAL: '수동 조정',
  RESERVATION: '예약 차감',
  RESERVATION_CANCEL: '예약 취소 복원',
};

// 백엔드 PtPassAdjustmentResponse 와 1:1 대응. 추가만 되고 고쳐지지 않는 이력이다.
export interface PtPassAdjustment {
  id: number;
  type: PtPassAdjustmentType;
  delta: number; // +2, -1
  remainingAfter: number; // 반영 직후 잔여 횟수
  reason: string;
  adjustedBy: string; // 처리한 계정 아이디 (서버가 로그인 정보에서 채운다)
  createdAt: string; // ISO-8601 UTC
}

export interface PtPassCreateRequest {
  sessionMinutes: SessionMinutes;
  totalCount: number;
}

// 더할 때는 양수, 뺄 때는 음수. 처리자는 서버가 채우므로 보내지 않는다.
export interface PtPassAdjustRequest {
  delta: number;
  reason: string;
}

// 서버(PtPass.MAX_PT_COUNT)와 같은 값. 오타(10 → 1000)로 인한 사고를 막는 상한이다.
export const MAX_PT_COUNT = 999;
