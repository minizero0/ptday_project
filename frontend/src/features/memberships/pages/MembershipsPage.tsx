import { useState } from 'react';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { formatDay } from '../../../lib/date';
import { MembershipCreateModal } from '../components/MembershipCreateModal';
import { MembershipEditModal } from '../components/MembershipEditModal';
import { MembershipStatusBadge } from '../components/MembershipStatusBadge';
import { useMembershipPageQuery } from '../hooks/useMembership';
import type { MembershipListItem } from '../types/membership';

const GRID_COLUMNS = ['회원번호', '이름', '상태', '시작일', '만료일', '남은 기간', ''] as const;

function formatRemaining(daysRemaining: number | null): string {
  if (daysRemaining == null) {
    return '-';
  }
  return daysRemaining === 0 ? '오늘 만료' : `${daysRemaining}일`;
}

export function MembershipsPage() {
  // 만료 포함 여부·페이지는 화면 로컬 상태 (CLAUDE.md §6)
  const [includeExpired, setIncludeExpired] = useState(false);
  const [page, setPage] = useState(0);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<MembershipListItem | null>(null);

  const { data, isPending, isPaused, isError } = useMembershipPageQuery(includeExpired, page);

  const handleToggleExpired = (checked: boolean) => {
    setIncludeExpired(checked);
    setPage(0); // 필터가 바뀌면 페이지 수가 달라지므로 첫 페이지로
  };

  return (
    <div className="flex h-full flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold">이용권관리</h1>
          <p className="mt-1 text-sm text-text-muted">
            {includeExpired
              ? '만료건까지 포함해 최근에 끝난 순으로 봅니다.'
              : '아직 끝나지 않은 이용권을 만료가 임박한 순으로 봅니다.'}
          </p>
        </div>

        <div className="flex items-center gap-4">
          <label className="flex cursor-pointer items-center gap-2 text-sm">
            <input
              type="checkbox"
              className="h-4 w-4 accent-primary"
              checked={includeExpired}
              onChange={(event) => handleToggleExpired(event.target.checked)}
            />
            만료건 포함
          </label>
          <Button onClick={() => setIsCreateOpen(true)}>이용권 등록</Button>
        </div>
      </div>

      <Card className="min-h-0 flex-1 overflow-auto p-0">
        <table className="w-full min-w-[720px] text-left text-sm">
          <thead>
            <tr className="border-b border-border text-text-muted">
              {GRID_COLUMNS.map((column) => (
                <th key={column} className="px-4 py-3 font-medium">
                  {column}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {isPending && (
              <tr>
                <td colSpan={GRID_COLUMNS.length} className="px-4 py-10 text-center text-text-muted">
                  {isPaused ? '네트워크 연결을 확인해주세요.' : '불러오는 중...'}
                </td>
              </tr>
            )}
            {isError && (
              <tr>
                <td colSpan={GRID_COLUMNS.length} className="px-4 py-10 text-center text-danger">
                  이용권 목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.
                </td>
              </tr>
            )}
            {data && data.content.length === 0 && (
              <tr>
                <td colSpan={GRID_COLUMNS.length} className="px-4 py-10 text-center text-text-muted">
                  {includeExpired ? '등록된 이용권이 없습니다.' : '이용중이거나 예정인 이용권이 없습니다.'}
                </td>
              </tr>
            )}
            {data?.content.map((membership: MembershipListItem) => (
              <tr key={membership.id} className="border-b border-border last:border-b-0">
                <td className="px-4 py-3">{membership.memberNo}</td>
                <td className="px-4 py-3 font-medium">{membership.memberName}</td>
                <td className="px-4 py-3">
                  <MembershipStatusBadge status={membership.status} />
                </td>
                <td className="px-4 py-3 text-text-muted">{formatDay(membership.startDate)}</td>
                <td className="px-4 py-3 text-text-muted">{formatDay(membership.endDate)}</td>
                <td className="px-4 py-3 text-text-muted">
                  {formatRemaining(membership.daysRemaining)}
                </td>
                <td className="px-4 py-3 text-right">
                  <Button variant="ghost" size="sm" onClick={() => setEditTarget(membership)}>
                    수정
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>

      {data && data.totalPages > 0 && (
        <div className="flex items-center justify-center gap-3 text-sm">
          <Button
            variant="ghost"
            size="sm"
            disabled={data.first}
            onClick={() => setPage((current) => current - 1)}
          >
            이전
          </Button>
          <span className="text-text-muted">
            {data.page + 1} / {data.totalPages} 페이지 · 총 {data.totalElements}건
          </span>
          <Button
            variant="ghost"
            size="sm"
            disabled={data.last}
            onClick={() => setPage((current) => current + 1)}
          >
            다음
          </Button>
        </div>
      )}

      {isCreateOpen && <MembershipCreateModal onClose={() => setIsCreateOpen(false)} />}
      {editTarget && (
        <MembershipEditModal membership={editTarget} onClose={() => setEditTarget(null)} />
      )}
    </div>
  );
}
