import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '../../../components/Button';
import { Modal } from '../../../components/Modal';
import { getApiErrorMessage } from '../../../lib/apiError';
import { useCreateMemberMutation } from '../hooks/useMember';
import { EMPTY_MEMBER_FORM, memberSchema, toMemberSaveRequest } from '../types/memberSchema';
import type { MemberFormValues } from '../types/memberSchema';
import { MemberFormFields } from './MemberFormFields';

interface MemberCreateModalProps {
  onClose: () => void;
  // 등록 성공 직후 호출. 목록을 첫 페이지로 돌려 새 회원이 보이게 하는 용도
  onCreated: () => void;
}

const CREATE_ERROR_FALLBACK = '회원 등록에 실패했습니다. 잠시 후 다시 시도해주세요.';

export function MemberCreateModal({ onClose, onCreated }: MemberCreateModalProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<MemberFormValues>({
    resolver: zodResolver(memberSchema),
    defaultValues: EMPTY_MEMBER_FORM,
  });

  const createMutation = useCreateMemberMutation();

  const onSubmit = (values: MemberFormValues) => {
    createMutation.mutate(toMemberSaveRequest(values), {
      onSuccess: () => {
        onCreated();
        onClose();
      },
    });
  };

  return (
    <Modal title="회원 등록" onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <MemberFormFields register={register} errors={errors} />

        <p className="text-xs text-text-muted">회원번호는 등록하면 자동으로 부여됩니다.</p>

        {createMutation.isError && (
          <p className="text-sm text-danger">
            {getApiErrorMessage(createMutation.error, CREATE_ERROR_FALLBACK)}
          </p>
        )}

        <div className="flex justify-end gap-2 border-t border-border pt-4">
          <Button type="button" variant="ghost" onClick={onClose}>
            취소
          </Button>
          <Button type="submit" isLoading={createMutation.isPending}>
            등록
          </Button>
        </div>
      </form>
    </Modal>
  );
}
