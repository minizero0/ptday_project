import { formatDay } from '../../../lib/date';
import { useMembershipsQuery } from '../hooks/useMembership';
import type { Membership } from '../types/membership';

interface MembershipHistoryProps {
  memberId: number;
}

function MembershipItem({ membership }: { membership: Membership }) {
  return (
    <li className="flex items-center justify-between gap-2 py-2">
      <p className="text-sm">
        {formatDay(membership.startDate)} ~ {formatDay(membership.endDate)}
      </p>
      {membership.active && (
        <span className="shrink-0 rounded-sm bg-success/10 px-2 py-0.5 text-xs font-semibold text-success">
          이용중
        </span>
      )}
    </li>
  );
}

/** 회원의 이용권 이력. 로딩/에러/빈 상태를 모두 표시한다 (CLAUDE.md §7.2). */
export function MembershipHistory({ memberId }: MembershipHistoryProps) {
  const { data: memberships, isPending, isPaused, isError } = useMembershipsQuery(memberId);

  // isLoading 이 아니라 isPending 으로 판정한다.
  // 재시도 대기(fetchStatus: paused) 구간에서는 isLoading 이 false 가 되는데,
  // 그때 "등록된 이용권이 없습니다"를 보여주면 조회 실패를 이용권 없음으로 오인하게 된다.
  if (isPending) {
    return (
      <p className="mt-2 text-sm text-text-muted">
        {isPaused ? '네트워크 연결을 확인해주세요.' : '불러오는 중...'}
      </p>
    );
  }

  if (isError) {
    return <p className="mt-2 text-sm text-danger">이용권 이력을 불러오지 못했습니다.</p>;
  }

  if (memberships.length === 0) {
    return <p className="mt-2 text-sm text-text-muted">등록된 이용권이 없습니다.</p>;
  }

  return (
    <ul className="mt-1 divide-y divide-border">
      {memberships.map((membership) => (
        <MembershipItem key={membership.id} membership={membership} />
      ))}
    </ul>
  );
}
