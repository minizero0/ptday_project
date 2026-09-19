import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '../../../components/Button';
import { Modal } from '../../../components/Modal';
import { getApiErrorMessage } from '../../../lib/apiError';
import { useUpdateMemberMutation } from '../hooks/useMember';
import type { Member } from '../types/member';
import { memberSchema, toMemberFormValues, toMemberSaveRequest } from '../types/memberSchema';
import type { MemberFormValues } from '../types/memberSchema';
import { MemberFormFields } from './MemberFormFields';

interface MemberEditModalProps {
  member: Member;
  onClose: () => void;
}

const UPDATE_ERROR_FALLBACK = '회원 정보 수정에 실패했습니다. 잠시 후 다시 시도해주세요.';

export function MemberEditModal({ member, onClose }: MemberEditModalProps) {
  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<MemberFormValues>({
    resolver: zodResolver(memberSchema),
    defaultValues: toMemberFormValues(member),
  });

  const updateMutation = useUpdateMemberMutation(member.id);

  const onSubmit = (values: MemberFormValues) => {
    updateMutation.mutate(toMemberSaveRequest(values), { onSuccess: onClose });
  };

  return (
    <Modal title="회원 정보 수정" onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        {/* 회원번호는 출석 체크 키라 바꿀 수 없다. 누구를 고치는지 확인용으로만 보여준다 */}
        <div className="rounded-md bg-background px-3 py-2">
          <p className="text-xs text-text-muted">회원번호</p>
          <p className="text-sm font-semibold">{member.memberNo}</p>
        </div>

        <MemberFormFields register={register} errors={errors} />

        {updateMutation.isError && (
          <p className="text-sm text-danger">
            {getApiErrorMessage(updateMutation.error, UPDATE_ERROR_FALLBACK)}
          </p>
        )}

        <div className="flex justify-end gap-2 border-t border-border pt-4">
          <Button type="button" variant="ghost" onClick={onClose}>
            취소
          </Button>
          {/* 바뀐 값이 없으면 보낼 이유가 없다 */}
          <Button type="submit" disabled={!isDirty} isLoading={updateMutation.isPending}>
            저장
          </Button>
        </div>
      </form>
    </Modal>
  );
}
