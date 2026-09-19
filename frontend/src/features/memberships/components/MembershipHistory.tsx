import { useState } from 'react';
import { Button } from '../../../components/Button';
import { formatDay } from '../../../lib/date';
import type { Member } from '../../members/types/member';
import { useMembershipsQuery } from '../hooks/useMembership';
import type { Membership } from '../types/membership';
import { MembershipEditModal } from './MembershipEditModal';

interface MembershipHistoryProps {
  // 수정 모달이 누구의 이용권인지 보여줘야 해서 id 가 아니라 회원을 받는다
  member: Member;
}

interface MembershipItemProps {
  membership: Membership;
  onEdit: (membership: Membership) => void;
}

function MembershipItem({ membership, onEdit }: MembershipItemProps) {
  return (
    <li className="flex items-center justify-between gap-2 py-2">
      <p className="text-sm">
        {formatDay(membership.startDate)} ~ {formatDay(membership.endDate)}
      </p>
      <div className="flex shrink-0 items-center gap-2">
        {membership.active && (
          <span className="rounded-sm bg-success/10 px-2 py-0.5 text-xs font-semibold text-success">
            이용중
          </span>
        )}
        <Button
          variant="ghost"
          size="sm"
          aria-label={`${formatDay(membership.startDate)} 시작 이용권 수정`}
          onClick={() => onEdit(membership)}
        >
          수정
        </Button>
      </div>
    </li>
  );
}

/** 회원의 이용권 이력. 로딩/에러/빈 상태를 모두 표시한다 (CLAUDE.md §7.2). */
export function MembershipHistory({ member }: MembershipHistoryProps) {
  const { data: memberships, isPending, isPaused, isError } = useMembershipsQuery(member.id);
  const [editTarget, setEditTarget] = useState<Membership | null>(null);

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
    <>
      <ul className="mt-1 divide-y divide-border">
        {memberships.map((membership) => (
          <MembershipItem key={membership.id} membership={membership} onEdit={setEditTarget} />
        ))}
      </ul>

      {editTarget && (
        <MembershipEditModal
          membership={{
            id: editTarget.id,
            memberNo: member.memberNo,
            memberName: member.name,
            startDate: editTarget.startDate,
            endDate: editTarget.endDate,
          }}
          onClose={() => setEditTarget(null)}
        />
      )}
    </>
  );
}
