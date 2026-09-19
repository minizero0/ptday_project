import { Button } from '../../../components/Button';
import { Modal } from '../../../components/Modal';
import { getApiErrorMessage } from '../../../lib/apiError';
import { useDeleteMemberMutation } from '../hooks/useMember';
import type { Member } from '../types/member';

interface MemberDeleteModalProps {
  member: Member;
  onClose: () => void;
  // 삭제 성공 직후 호출. 페이지의 마지막 한 명을 지웠을 때 앞 페이지로 옮기는 용도
  onDeleted: () => void;
}

const DELETE_ERROR_FALLBACK = '회원 삭제에 실패했습니다. 잠시 후 다시 시도해주세요.';

/** 삭제는 되돌리는 화면이 없으므로 누구를 지우는지 한 번 더 확인받는다. */
export function MemberDeleteModal({ member, onClose, onDeleted }: MemberDeleteModalProps) {
  const deleteMutation = useDeleteMemberMutation();

  const handleDelete = () => {
    deleteMutation.mutate(member.id, {
      onSuccess: () => {
        onDeleted();
        onClose();
      },
    });
  };

  return (
    <Modal title="회원 삭제" onClose={onClose}>
      <div className="space-y-4">
        <div className="rounded-md bg-background px-3 py-2">
          <p className="text-sm font-semibold">{member.name}</p>
          <p className="text-xs text-text-muted">
            {member.memberNo} · {member.phone ?? '연락처 없음'}
          </p>
        </div>

        <p className="text-sm">
          이 회원을 삭제할까요? 삭제하면 회원 목록과 검색, 출석 체크에서 더 이상 나타나지 않습니다.
        </p>

        {deleteMutation.isError && (
          <p className="text-sm text-danger">
            {getApiErrorMessage(deleteMutation.error, DELETE_ERROR_FALLBACK)}
          </p>
        )}

        <div className="flex justify-end gap-2 border-t border-border pt-4">
          <Button variant="ghost" onClick={onClose}>
            취소
          </Button>
          <Button variant="danger" onClick={handleDelete} isLoading={deleteMutation.isPending}>
            삭제
          </Button>
        </div>
      </div>
    </Modal>
  );
}
