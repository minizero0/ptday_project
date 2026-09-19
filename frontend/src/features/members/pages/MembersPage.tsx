import { useState } from 'react';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { Input } from '../../../components/Input';
import { useDebounce } from '../../../hooks/useDebounce';
import { formatDay, formatInstantDay } from '../../../lib/date';
import { useAuthStore } from '../../auth/store/authStore';
import { ROLE_ADMIN } from '../../auth/types/auth';
import { MemberCreateModal } from '../components/MemberCreateModal';
import { MemberDeleteModal } from '../components/MemberDeleteModal';
import { MemberEditModal } from '../components/MemberEditModal';
import { useMemberPageQuery } from '../hooks/useMember';
import type { Member } from '../types/member';

const GRID_COLUMNS = ['회원번호', '이름', '전화번호', '성별', '생년월일', '등록일', ''] as const;
const SEARCH_DEBOUNCE_MS = 250;
const EMPTY_VALUE = '-';
const DELETE_FORBIDDEN_HINT = '회원 삭제는 관리자만 할 수 있습니다.';

export function MembersPage() {
  // 검색어·페이지는 화면 로컬 상태 (CLAUDE.md §6)
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<Member | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Member | null>(null);

  // 화면 제어용일 뿐이다. 실제 권한은 서버가 DELETE 요청에서 다시 확인한다.
  const canDelete = useAuthStore((state) => state.role === ROLE_ADMIN);

  // 타이핑마다 요청하지 않도록 입력이 멈춘 뒤에만 검색한다
  const debouncedKeyword = useDebounce(keyword, SEARCH_DEBOUNCE_MS);
  const isSearching = debouncedKeyword.trim().length > 0;

  const { data, isPending, isPaused, isError } = useMemberPageQuery(debouncedKeyword, page);

  // 페이지에 한 명만 남은 상태에서 지우면 그 페이지가 사라지므로 앞 페이지로 옮긴다
  const handleDeleted = () => {
    const isLastRowOnPage = data?.content.length === 1;
    if (isLastRowOnPage && page > 0) {
      setPage(page - 1);
    }
  };

  const handleKeywordChange = (value: string) => {
    setKeyword(value);
    setPage(0); // 검색어가 바뀌면 결과 페이지 수가 달라지므로 첫 페이지로
  };

  return (
    <div className="flex h-full flex-col gap-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold">회원관리</h1>
          <p className="mt-1 text-sm text-text-muted">최근에 등록한 회원 순으로 봅니다.</p>
        </div>

        <div className="flex w-full items-center gap-3 sm:w-auto">
          <div className="min-w-0 flex-1 sm:w-72 sm:flex-none">
            <Input
              type="search"
              aria-label="회원 검색"
              placeholder="이름, 회원번호, 전화번호"
              value={keyword}
              onChange={(event) => handleKeywordChange(event.target.value)}
            />
          </div>
          <Button className="shrink-0" onClick={() => setIsCreateOpen(true)}>
            회원 등록
          </Button>
        </div>
      </div>

      <Card className="min-h-0 flex-1 overflow-auto p-0">
        <table className="w-full min-w-[840px] text-left text-sm">
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
                  회원 목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.
                </td>
              </tr>
            )}
            {data && data.content.length === 0 && (
              <tr>
                <td colSpan={GRID_COLUMNS.length} className="px-4 py-10 text-center text-text-muted">
                  {isSearching ? '검색 결과가 없습니다.' : '등록된 회원이 없습니다.'}
                </td>
              </tr>
            )}
            {data?.content.map((member: Member) => (
              <tr key={member.id} className="border-b border-border last:border-b-0">
                <td className="px-4 py-3">{member.memberNo}</td>
                <td className="px-4 py-3 font-medium">{member.name}</td>
                <td className="px-4 py-3 text-text-muted">{member.phone ?? EMPTY_VALUE}</td>
                <td className="px-4 py-3 text-text-muted">{member.gender ?? EMPTY_VALUE}</td>
                <td className="px-4 py-3 text-text-muted">
                  {member.birthDate ? formatDay(member.birthDate) : EMPTY_VALUE}
                </td>
                <td className="px-4 py-3 text-text-muted">{formatInstantDay(member.createdAt)}</td>
                <td className="px-4 py-3">
                  <div className="flex justify-end gap-2 whitespace-nowrap">
                    <Button variant="ghost" size="sm" onClick={() => setEditTarget(member)}>
                      수정
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-danger"
                      disabled={!canDelete}
                      title={canDelete ? undefined : DELETE_FORBIDDEN_HINT}
                      onClick={() => setDeleteTarget(member)}
                    >
                      삭제
                    </Button>
                  </div>
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
            {data.page + 1} / {data.totalPages} 페이지 · 총 {data.totalElements}명
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

      {isCreateOpen && (
        <MemberCreateModal
          onClose={() => setIsCreateOpen(false)}
          // 최근 등록순 목록이라 새 회원은 첫 페이지 맨 위에 나타난다
          onCreated={() => setPage(0)}
        />
      )}
      {editTarget && <MemberEditModal member={editTarget} onClose={() => setEditTarget(null)} />}
      {deleteTarget && (
        <MemberDeleteModal
          member={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onDeleted={handleDeleted}
        />
      )}
    </div>
  );
}
