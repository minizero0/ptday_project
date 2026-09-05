import type { MembershipPeriodStatus } from '../types/membership';

interface MembershipStatusBadgeProps {
  status: MembershipPeriodStatus;
}

const STATUS_STYLES: Record<MembershipPeriodStatus, { label: string; className: string }> = {
  ACTIVE: { label: '이용중', className: 'bg-success/10 text-success' },
  SCHEDULED: { label: '예정', className: 'bg-info/10 text-info' },
  EXPIRED: { label: '만료', className: 'bg-background text-text-muted' },
};

/** 이용권 기간 상태 배지. 상태 판단은 서버가 하고 여기서는 라벨/색만 매핑한다. */
export function MembershipStatusBadge({ status }: MembershipStatusBadgeProps) {
  const { label, className } = STATUS_STYLES[status];

  return (
    <span className={`rounded-sm px-2 py-0.5 text-xs font-semibold ${className}`}>{label}</span>
  );
}
