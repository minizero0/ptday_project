import { useState } from 'react';
import { Button } from '../../../components/Button';
import { Input } from '../../../components/Input';
import { Modal } from '../../../components/Modal';
import { useDebounce } from '../../../hooks/useDebounce';
import { addMonths, todayString } from '../../../lib/date';
import { useMemberSearchQuery } from '../../members/hooks/useMember';
import type { Member } from '../../members/types/member';
import { useGrantMembershipMutation } from '../hooks/useMembership';
import type { MembershipPeriodRequest } from '../types/membership';
import { MembershipFormFields } from './MembershipFormFields';

interface MembershipCreateModalProps {
  // 회원이 이미 정해진 곳(회원 상세)에서 열면 검색 단계를 건너뛰고 그 회원으로 고정한다.
  member?: Member;
  onClose: () => void;
}

const DEFAULT_MONTHS = 1;
const SEARCH_DEBOUNCE_MS = 250;

// 서버가 내려준 친절한 메시지를 그대로 보여준다 (CLAUDE.md §7.2)
function toErrorMessage(error: unknown): string {
  const response = (error as { response?: { data?: { error?: { message?: string } } } })?.response;
  return response?.data?.error?.message ?? '이용권 등록에 실패했습니다. 잠시 후 다시 시도해주세요.';
}

export function MembershipCreateModal({ member, onClose }: MembershipCreateModalProps) {
  const isMemberFixed = member !== undefined;
  const [keyword, setKeyword] = useState('');
  const [selectedMember, setSelectedMember] = useState<Member | null>(member ?? null);
  const [form, setForm] = useState<MembershipPeriodRequest>(() => {
    const startDate = todayString();
    return { startDate, endDate: addMonths(startDate, DEFAULT_MONTHS) };
  });

  const debouncedKeyword = useDebounce(keyword, SEARCH_DEBOUNCE_MS);
  const { data: searchResult, isFetching } = useMemberSearchQuery(debouncedKeyword);

  const grantMutation = useGrantMembershipMutation(selectedMember?.id ?? -1);

  const handleSubmit = () => {
    if (!selectedMember) {
      return;
    }
    grantMutation.mutate(form, { onSuccess: onClose });
  };

  return (
    <Modal title="이용권 등록" onClose={onClose}>
      <div className="space-y-4">
        {selectedMember ? (
          <div className="flex items-center justify-between rounded-md bg-background px-3 py-2">
            <div>
              <p className="text-sm font-semibold">{selectedMember.name}</p>
              <p className="text-xs text-text-muted">
                {selectedMember.memberNo} · {selectedMember.phone ?? '연락처 없음'}
              </p>
            </div>
            {!isMemberFixed && (
              <Button variant="ghost" size="sm" onClick={() => setSelectedMember(null)}>
                변경
              </Button>
            )}
          </div>
        ) : (
          <div>
            <Input
              label="회원 검색"
              placeholder="이름, 회원번호, 전화번호"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
            <div className="mt-2 max-h-48 overflow-y-auto rounded-md border border-border">
              {keyword.trim().length === 0 && (
                <p className="px-3 py-4 text-center text-sm text-text-muted">
                  검색어를 입력하세요.
                </p>
              )}
              {keyword.trim().length > 0 && isFetching && (
                <p className="px-3 py-4 text-center text-sm text-text-muted">검색 중...</p>
              )}
              {keyword.trim().length > 0 && !isFetching && searchResult?.content.length === 0 && (
                <p className="px-3 py-4 text-center text-sm text-text-muted">
                  검색 결과가 없습니다.
                </p>
              )}
              {!isFetching &&
                searchResult?.content.map((member) => (
                  <button
                    key={member.id}
                    type="button"
                    onClick={() => setSelectedMember(member)}
                    className="flex w-full items-center justify-between border-b border-border px-3 py-2 text-left last:border-b-0 hover:bg-background"
                  >
                    <span className="text-sm font-medium">{member.name}</span>
                    <span className="text-xs text-text-muted">
                      {member.memberNo} · {member.phone ?? '-'}
                    </span>
                  </button>
                ))}
            </div>
          </div>
        )}

        <MembershipFormFields value={form} onChange={setForm} />

        {grantMutation.isError && (
          <p className="text-sm text-danger">{toErrorMessage(grantMutation.error)}</p>
        )}

        <div className="flex justify-end gap-2 border-t border-border pt-4">
          <Button variant="ghost" onClick={onClose}>
            취소
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={!selectedMember || grantMutation.isPending}
            isLoading={grantMutation.isPending}
          >
            등록
          </Button>
        </div>
      </div>
    </Modal>
  );
}
