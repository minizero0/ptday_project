import { useState } from 'react';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { formatDay } from '../../../lib/date';
import { MemberDetailPanel } from '../../members/components/MemberDetailPanel';
import { MembershipStatusBadge } from '../../memberships/components/MembershipStatusBadge';
import { useDailyAttendancesQuery } from '../hooks/useAttendance';
import type { Attendance } from '../types/attendance';

const WEEKDAY_LABELS = ['일', '월', '화', '수', '목', '금', '토'] as const;

// 로컬(기기) 기준 yyyy-MM-dd — 서버가 한국 시간대 기준으로 하루를 계산한다
function toDateParam(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatDateLabel(dateParam: string): string {
  const date = new Date(`${dateParam}T00:00:00`);
  return `${dateParam} (${WEEKDAY_LABELS[date.getDay()]})`;
}

// ISO-8601 UTC → 로컬 HH:mm (CLAUDE.md §5: 표시할 때만 로컬 변환)
function formatTime(checkedInAt: string): string {
  return new Date(checkedInAt).toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
}

function shiftDate(dateParam: string, days: number): string {
  const date = new Date(`${dateParam}T00:00:00`);
  date.setDate(date.getDate() + days);
  return toDateParam(date);
}

const GRID_COLUMNS = [
  '시간',
  '회원번호',
  '이름',
  '이용권 상태',
  '이용권 시작일',
  '이용권 만료일',
  'PT 잔여',
] as const;

export function AttendancePage() {
  const [date, setDate] = useState(() => toDateParam(new Date()));
  // 행 클릭으로 선택된 회원. 같은 행 재클릭 시 해제 (로컬 UI 상태 — useState)
  const [selectedMemberId, setSelectedMemberId] = useState<number | null>(null);
  const { data, isLoading, isError } = useDailyAttendancesQuery(date);

  const isToday = date === toDateParam(new Date());

  const handleRowClick = (memberId: number) => {
    setSelectedMemberId((current) => (current === memberId ? null : memberId));
  };

  return (
    <div className="flex h-full flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold">출석 현황</h1>
          <p className="mt-1 text-sm text-text-muted">
            {isToday ? '오늘' : formatDateLabel(date)} 출석{' '}
            <span className="font-semibold text-text-primary">{data?.totalCount ?? '-'}명</span>
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="sm"
            aria-label="이전 날짜"
            onClick={() => setDate(shiftDate(date, -1))}
          >
            ◀
          </Button>
          <label className="sr-only" htmlFor="attendance-date">
            조회 날짜
          </label>
          <input
            id="attendance-date"
            type="date"
            value={date}
            onChange={(event) => event.target.value && setDate(event.target.value)}
            className="rounded-md border border-border bg-surface px-3 py-1.5 text-sm"
          />
          <Button
            variant="ghost"
            size="sm"
            aria-label="다음 날짜"
            onClick={() => setDate(shiftDate(date, 1))}
          >
            ▶
          </Button>
          {!isToday && (
            <Button variant="ghost" size="sm" onClick={() => setDate(toDateParam(new Date()))}>
              오늘
            </Button>
          )}
        </div>
      </div>

      <div className="flex min-h-0 flex-1 items-stretch gap-4">
        <Card className="min-w-0 flex-1 overflow-auto p-0">
        <table className="w-full min-w-[640px] text-left text-sm">
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
            {isLoading && (
              <tr>
                <td colSpan={GRID_COLUMNS.length} className="px-4 py-10 text-center text-text-muted">
                  불러오는 중...
                </td>
              </tr>
            )}
            {isError && (
              <tr>
                <td colSpan={GRID_COLUMNS.length} className="px-4 py-10 text-center text-danger">
                  출석 현황을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.
                </td>
              </tr>
            )}
            {data && data.items.length === 0 && (
              <tr>
                <td colSpan={GRID_COLUMNS.length} className="px-4 py-10 text-center text-text-muted">
                  {formatDateLabel(date)} 출석 기록이 없습니다.
                </td>
              </tr>
            )}
            {data?.items.map((attendance: Attendance) => (
              <tr
                key={attendance.id}
                onClick={() => handleRowClick(attendance.memberId)}
                className={`cursor-pointer border-b border-border last:border-b-0 ${
                  selectedMemberId === attendance.memberId
                    ? 'bg-primary/10'
                    : 'hover:bg-background'
                }`}
              >
                <td className="px-4 py-3 font-medium">{formatTime(attendance.checkedInAt)}</td>
                <td className="px-4 py-3">{attendance.memberNo}</td>
                <td className="px-4 py-3">{attendance.name}</td>
                <td className="px-4 py-3">
                  {attendance.membership ? (
                    <MembershipStatusBadge status={attendance.membership.status} />
                  ) : (
                    <span className="text-text-muted">-</span>
                  )}
                </td>
                <td className="px-4 py-3 text-text-muted">
                  {attendance.membership ? formatDay(attendance.membership.startDate) : '-'}
                </td>
                <td className="px-4 py-3 text-text-muted">
                  {attendance.membership ? formatDay(attendance.membership.endDate) : '-'}
                </td>
                {/* PT 잔여는 PT권 도메인(3차) 구현 후 채운다 */}
                <td className="px-4 py-3 text-text-muted">-</td>
              </tr>
            ))}
          </tbody>
        </table>
        </Card>

        <MemberDetailPanel
          memberId={selectedMemberId}
          onClose={() => setSelectedMemberId(null)}
        />
      </div>
    </div>
  );
}
