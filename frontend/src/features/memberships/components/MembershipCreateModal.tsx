import { useState } from 'react';
import { Button } from '../../../components/Button';
import { Modal } from '../../../components/Modal';
import { addMonths, todayString } from '../../../lib/date';
import { MemberPicker } from '../../members/components/MemberPicker';
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

// 서버가 내려준 친절한 메시지를 그대로 보여준다 (CLAUDE.md §7.2)
function toErrorMessage(error: unknown): string {
  const response = (error as { response?: { data?: { error?: { message?: string } } } })?.response;
  return response?.data?.error?.message ?? '이용권 등록에 실패했습니다. 잠시 후 다시 시도해주세요.';
}

export function MembershipCreateModal({ member, onClose }: MembershipCreateModalProps) {
  const isMemberFixed = member !== undefined;
  const [selectedMember, setSelectedMember] = useState<Member | null>(member ?? null);
  const [form, setForm] = useState<MembershipPeriodRequest>(() => {
    const startDate = todayString();
    return { startDate, endDate: addMonths(startDate, DEFAULT_MONTHS) };
  });

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
          <MemberPicker onSelect={setSelectedMember} />
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
