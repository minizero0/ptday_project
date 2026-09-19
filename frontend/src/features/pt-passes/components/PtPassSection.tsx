import { useState } from 'react';
import { Button } from '../../../components/Button';
import { formatInstantDay } from '../../../lib/date';
import type { Member } from '../../members/types/member';
import { usePtPassesQuery } from '../hooks/usePtPass';
import type { PtPass } from '../types/ptPass';
import { PtPassAdjustModal } from './PtPassAdjustModal';
import { PtPassCreateModal } from './PtPassCreateModal';

interface PtPassSectionProps {
  member: Member;
}

interface PtPassItemProps {
  ptPass: PtPass;
  onAdjust: (ptPass: PtPass) => void;
}

function PtPassItem({ ptPass, onAdjust }: PtPassItemProps) {
  return (
    <li className="flex items-center justify-between gap-2 py-2">
      <div>
        <p className="text-sm">
          잔여 <span className="font-semibold">{ptPass.remainingCount}회</span>
          <span className="text-text-muted"> · 구매 {ptPass.totalCount}회</span>
        </p>
        <p className="mt-0.5 text-xs text-text-muted">{formatInstantDay(ptPass.createdAt)} 등록</p>
      </div>
      <Button
        variant="ghost"
        size="sm"
        aria-label={`구매 ${ptPass.totalCount}회 PT권(잔여 ${ptPass.remainingCount}회, ${formatInstantDay(ptPass.createdAt)} 등록) 조정·이력`}
        onClick={() => onAdjust(ptPass)}
      >
        조정·이력
      </Button>
    </li>
  );
}

function PtPassList({ member, onAdjust }: PtPassSectionProps & Pick<PtPassItemProps, 'onAdjust'>) {
  const { data: ptPasses, isPending, isPaused, isError } = usePtPassesQuery(member.id);

  // isLoading 이 아니라 isPending 으로 판정한다 — 재시도 대기 구간에서 "PT권 없음"으로 오인하지 않게 (이용권 이력과 같은 이유)
  if (isPending) {
    return (
      <p className="mt-2 text-sm text-text-muted">
        {isPaused ? '네트워크 연결을 확인해주세요.' : '불러오는 중...'}
      </p>
    );
  }

  if (isError) {
    return <p className="mt-2 text-sm text-danger">PT권을 불러오지 못했습니다.</p>;
  }

  if (ptPasses.length === 0) {
    return <p className="mt-2 text-sm text-text-muted">등록된 PT권이 없습니다.</p>;
  }

  const totalRemaining = ptPasses.reduce((sum, ptPass) => sum + ptPass.remainingCount, 0);

  return (
    <>
      <p className="mt-2 rounded-md bg-background px-3 py-2 text-sm">
        PT 잔여 합계 <span className="font-semibold">{totalRemaining}회</span>
      </p>
      <ul className="mt-1 divide-y divide-border">
        {ptPasses.map((ptPass) => (
          <PtPassItem key={ptPass.id} ptPass={ptPass} onAdjust={onAdjust} />
        ))}
      </ul>
    </>
  );
}

/** 회원 상세 패널의 PT권 영역: 잔여 합계, PT권 목록, 등록, 횟수 조정·이력. */
export function PtPassSection({ member }: PtPassSectionProps) {
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [adjustTargetId, setAdjustTargetId] = useState<number | null>(null);
  const { data: ptPasses } = usePtPassesQuery(member.id);

  // id 로 들고 있다가 최신 목록에서 다시 찾는다. 객체를 그대로 들고 있으면 조정 직후에도 옛 잔여 횟수가 보인다.
  const adjustTarget = ptPasses?.find((ptPass) => ptPass.id === adjustTargetId) ?? null;

  return (
    <div className="mt-4 border-t border-border pt-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold">PT권</h3>
        <Button size="sm" onClick={() => setIsCreateOpen(true)}>
          PT권 등록
        </Button>
      </div>

      <PtPassList member={member} onAdjust={(ptPass) => setAdjustTargetId(ptPass.id)} />

      {isCreateOpen && <PtPassCreateModal member={member} onClose={() => setIsCreateOpen(false)} />}
      {adjustTarget && (
        <PtPassAdjustModal
          member={member}
          ptPass={adjustTarget}
          onClose={() => setAdjustTargetId(null)}
        />
      )}
    </div>
  );
}
