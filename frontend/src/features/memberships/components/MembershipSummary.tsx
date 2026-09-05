import { formatDay } from '../../../lib/date';
import { useMembershipsQuery } from '../hooks/useMembership';
import type { Membership } from '../types/membership';

interface MembershipSummaryProps {
  memberId: number;
}

// 이용중인 이용권 중 가장 늦게 끝나는 것. 갱신 구매로 유효 기간이 겹칠 수 있어 max 로 고른다.
function findCurrentMembership(memberships: Membership[]): Membership | null {
  const active = memberships.filter((membership) => membership.active);
  if (active.length === 0) {
    return null;
  }
  return active.reduce((latest, membership) =>
    membership.endDate > latest.endDate ? membership : latest,
  );
}

// 남은 일수가 없으면 빈 문자열 — 서버가 값을 안 주는 경우에도 "D-undefined" 가 뜨지 않게 한다.
function formatRemaining(daysRemaining: number | null): string {
  if (daysRemaining == null) {
    return '';
  }
  return daysRemaining === 0 ? '오늘 만료' : `D-${daysRemaining}`;
}

/**
 * 현재 이용권 요약. 데스크에서 가장 먼저 봐야 하는 정보라 패널 상단에 고정한다.
 * 로딩/에러 문구는 바로 아래 이력 섹션이 이미 보여주므로 여기서는 그리지 않는다(중복 방지).
 */
export function MembershipSummary({ memberId }: MembershipSummaryProps) {
  const { data: memberships, isPending, isError } = useMembershipsQuery(memberId);

  if (isPending || isError) {
    return null;
  }

  const current = findCurrentMembership(memberships);
  const remaining = current ? formatRemaining(current.daysRemaining) : '';

  return (
    <div className="sticky top-0 z-10 -mx-4 mb-1 bg-surface px-4 pb-2 pt-1">
      {current ? (
        <div className="rounded-md bg-success/10 px-3 py-2">
          <p className="text-sm font-semibold text-success">
            이용중{remaining && ` · ${remaining}`}
          </p>
          <p className="mt-0.5 text-xs text-text-muted">
            {formatDay(current.startDate)} ~ {formatDay(current.endDate)}
          </p>
        </div>
      ) : (
        <div className="rounded-md bg-background px-3 py-2">
          <p className="text-sm font-semibold text-text-muted">이용중인 이용권 없음</p>
        </div>
      )}
    </div>
  );
}
