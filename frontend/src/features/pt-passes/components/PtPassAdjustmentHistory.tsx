import { usePtPassAdjustmentsQuery } from '../hooks/usePtPass';
import { ADJUSTMENT_TYPE_LABELS } from '../types/ptPass';
import type { PtPassAdjustment } from '../types/ptPass';

interface PtPassAdjustmentHistoryProps {
  ptPassId: number;
}

// ISO-8601 UTC → 로컬 날짜·시각. 다툼이 생겼을 때 "언제"가 중요해 시각까지 보여준다 (CLAUDE.md §5)
function formatInstant(instant: string): string {
  return new Date(instant).toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function AdjustmentItem({ adjustment }: { adjustment: PtPassAdjustment }) {
  const isAddition = adjustment.delta > 0;

  return (
    <li className="py-2">
      <div className="flex items-center justify-between gap-2">
        <p className={`text-sm font-semibold ${isAddition ? 'text-success' : 'text-danger'}`}>
          {isAddition ? `+${adjustment.delta}` : adjustment.delta}회
        </p>
        <p className="text-xs text-text-muted">
          {ADJUSTMENT_TYPE_LABELS[adjustment.type]} · 잔여 {adjustment.remainingAfter}회
        </p>
      </div>
      <p className="mt-0.5 text-sm">{adjustment.reason}</p>
      <p className="mt-0.5 text-xs text-text-muted">
        {adjustment.adjustedBy} · {formatInstant(adjustment.createdAt)}
      </p>
    </li>
  );
}

/** PT권의 횟수 변동 이력. 로딩/에러/빈 상태를 모두 표시한다 (CLAUDE.md §7.2). */
export function PtPassAdjustmentHistory({ ptPassId }: PtPassAdjustmentHistoryProps) {
  const { data: adjustments, isPending, isPaused, isError } = usePtPassAdjustmentsQuery(ptPassId);

  if (isPending) {
    return (
      <p className="mt-2 text-sm text-text-muted">
        {isPaused ? '네트워크 연결을 확인해주세요.' : '불러오는 중...'}
      </p>
    );
  }

  if (isError) {
    return <p className="mt-2 text-sm text-danger">변동 이력을 불러오지 못했습니다.</p>;
  }

  if (adjustments.length === 0) {
    return <p className="mt-2 text-sm text-text-muted">아직 변동 이력이 없습니다.</p>;
  }

  return (
    <ul className="mt-1 max-h-48 divide-y divide-border overflow-y-auto">
      {adjustments.map((adjustment) => (
        <AdjustmentItem key={adjustment.id} adjustment={adjustment} />
      ))}
    </ul>
  );
}
