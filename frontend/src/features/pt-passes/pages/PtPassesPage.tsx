import { useState } from 'react';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { Input } from '../../../components/Input';
import { useDebounce } from '../../../hooks/useDebounce';
import { getApiErrorMessage } from '../../../lib/apiError';
import { formatInstantDay } from '../../../lib/date';
import { PtPassAdjustModal } from '../components/PtPassAdjustModal';
import { PtPassCreateModal } from '../components/PtPassCreateModal';
import { usePtPassPageQuery } from '../hooks/usePtPass';

const GRID_COLUMNS = ['회원번호', '이름', '수업 길이', '구매 횟수', '잔여 횟수', '구매일', ''] as const;
const NAME_DEBOUNCE_MS = 250;
const LIST_ERROR_FALLBACK = 'PT권 목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.';

export function PtPassesPage() {
  // 검색 조건·페이지는 화면 로컬 상태 (CLAUDE.md §6)
  const [includeExhausted, setIncludeExhausted] = useState(false);
  const [name, setName] = useState('');
  const [purchasedFrom, setPurchasedFrom] = useState('');
  const [purchasedTo, setPurchasedTo] = useState('');
  const [page, setPage] = useState(0);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [adjustTargetId, setAdjustTargetId] = useState<number | null>(null);

  // 글자를 칠 때마다 요청하지 않도록 이름만 늦춰서 보낸다
  const debouncedName = useDebounce(name.trim(), NAME_DEBOUNCE_MS);
  const { data, isPending, isPaused, isError, error } = usePtPassPageQuery(
    { includeExhausted, name: debouncedName, purchasedFrom, purchasedTo },
    page,
  );

  const hasSearchCondition = name.trim() !== '' || purchasedFrom !== '' || purchasedTo !== '';

  // id 로 들고 있다가 최신 목록에서 다시 찾는다. 객체를 그대로 들고 있으면 조정 직후에도 옛 잔여 횟수가 보인다.
  const adjustTarget = data?.content.find((ptPass) => ptPass.id === adjustTargetId) ?? null;

  // 조건이 바뀌면 페이지 수가 달라지므로 첫 페이지로 돌린다
  const handleToggleExhausted = (checked: boolean) => {
    setIncludeExhausted(checked);
    setPage(0);
  };

  const handleNameChange = (value: string) => {
    setName(value);
    setPage(0);
  };

  const handlePurchasedFromChange = (value: string) => {
    setPurchasedFrom(value);
    setPage(0);
  };

  const handlePurchasedToChange = (value: string) => {
    setPurchasedTo(value);
    setPage(0);
  };

  const handleResetSearch = () => {
    setName('');
    setPurchasedFrom('');
    setPurchasedTo('');
    setPage(0);
  };

  const emptyMessageWithoutSearch = includeExhausted
    ? '등록된 PT권이 없습니다.'
    : '잔여 횟수가 남은 PT권이 없습니다.';
  const emptyMessage = hasSearchCondition ? '조건에 맞는 PT권이 없습니다.' : emptyMessageWithoutSearch;

  return (
    <div className="flex h-full flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold">PT권관리</h1>
          <p className="mt-1 text-sm text-text-muted">
            {includeExhausted
              ? '다 쓴 PT권까지 포함해 최근 구매순으로 봅니다.'
              : '잔여 횟수가 남은 PT권을 최근 구매순으로 봅니다.'}
          </p>
        </div>

        <div className="flex items-center gap-4">
          <label className="flex cursor-pointer items-center gap-2 text-sm">
            <input
              type="checkbox"
              className="h-4 w-4 accent-primary"
              checked={includeExhausted}
              onChange={(event) => handleToggleExhausted(event.target.checked)}
            />
            소진건 포함
          </label>
          <Button onClick={() => setIsCreateOpen(true)}>PT권 등록</Button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-[minmax(0,1fr)_auto_auto_auto] lg:items-end">
        <Input
          label="이름"
          type="search"
          placeholder="회원 이름"
          value={name}
          onChange={(event) => handleNameChange(event.target.value)}
        />
        <Input
          label="구매일 시작"
          type="date"
          max={purchasedTo || undefined}
          value={purchasedFrom}
          onChange={(event) => handlePurchasedFromChange(event.target.value)}
        />
        <Input
          label="구매일 종료"
          type="date"
          min={purchasedFrom || undefined}
          value={purchasedTo}
          onChange={(event) => handlePurchasedToChange(event.target.value)}
        />
        <Button variant="ghost" disabled={!hasSearchCondition} onClick={handleResetSearch}>
          초기화
        </Button>
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
                  {getApiErrorMessage(error, LIST_ERROR_FALLBACK)}
                </td>
              </tr>
            )}
            {!isError && data && data.content.length === 0 && (
              <tr>
                <td colSpan={GRID_COLUMNS.length} className="px-4 py-10 text-center text-text-muted">
                  {emptyMessage}
                </td>
              </tr>
            )}
            {data?.content.map((ptPass) => (
              <tr key={ptPass.id} className="border-b border-border last:border-b-0">
                <td className="px-4 py-3">{ptPass.memberNo}</td>
                <td className="px-4 py-3 font-medium">{ptPass.memberName}</td>
                <td className="px-4 py-3">{ptPass.sessionMinutes}분</td>
                <td className="px-4 py-3 text-text-muted">{ptPass.totalCount}회</td>
                <td className="px-4 py-3 font-semibold">{ptPass.remainingCount}회</td>
                <td className="px-4 py-3 text-text-muted">{formatInstantDay(ptPass.createdAt)}</td>
                <td className="px-4 py-3 text-right">
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label={`${ptPass.memberName} ${ptPass.sessionMinutes}분 PT권(잔여 ${ptPass.remainingCount}회) 조정·이력`}
                    onClick={() => setAdjustTargetId(ptPass.id)}
                  >
                    조정·이력
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

      {isCreateOpen && <PtPassCreateModal onClose={() => setIsCreateOpen(false)} />}
      {adjustTarget && (
        <PtPassAdjustModal
          member={{ name: adjustTarget.memberName, memberNo: adjustTarget.memberNo }}
          ptPass={adjustTarget}
          onClose={() => setAdjustTargetId(null)}
        />
      )}
    </div>
  );
}
