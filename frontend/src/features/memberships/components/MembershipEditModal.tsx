import { useState } from 'react';
import { Button } from '../../../components/Button';
import { Modal } from '../../../components/Modal';
import { useUpdateMembershipMutation } from '../hooks/useMembership';
import type { MembershipListItem, MembershipPeriodRequest } from '../types/membership';
import { MembershipFormFields } from './MembershipFormFields';

interface MembershipEditModalProps {
  membership: MembershipListItem;
  onClose: () => void;
}

// 서버가 내려준 친절한 메시지를 그대로 보여준다 (CLAUDE.md §7.2)
function toErrorMessage(error: unknown): string {
  const response = (error as { response?: { data?: { error?: { message?: string } } } })?.response;
  return response?.data?.error?.message ?? '이용권 수정에 실패했습니다. 잠시 후 다시 시도해주세요.';
}

export function MembershipEditModal({ membership, onClose }: MembershipEditModalProps) {
  const [form, setForm] = useState<MembershipPeriodRequest>({
    startDate: membership.startDate,
    endDate: membership.endDate,
  });

  const updateMutation = useUpdateMembershipMutation(membership.id);

  return (
    <Modal title="이용권 수정" onClose={onClose}>
      <div className="space-y-4">
        <div className="rounded-md bg-background px-3 py-2">
          <p className="text-sm font-semibold">{membership.memberName}</p>
          <p className="text-xs text-text-muted">{membership.memberNo}</p>
        </div>

        <MembershipFormFields value={form} onChange={setForm} />

        {updateMutation.isError && (
          <p className="text-sm text-danger">{toErrorMessage(updateMutation.error)}</p>
        )}

        <div className="flex justify-end gap-2 border-t border-border pt-4">
          <Button variant="ghost" onClick={onClose}>
            취소
          </Button>
          <Button
            onClick={() => updateMutation.mutate(form, { onSuccess: onClose })}
            disabled={updateMutation.isPending}
            isLoading={updateMutation.isPending}
          >
            저장
          </Button>
        </div>
      </div>
    </Modal>
  );
}
