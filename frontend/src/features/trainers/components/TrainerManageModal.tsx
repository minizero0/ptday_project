import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Button } from '../../../components/Button';
import { Input } from '../../../components/Input';
import { Modal } from '../../../components/Modal';
import { getApiErrorMessage } from '../../../lib/apiError';
import { formatPhone } from '../../../lib/phone';
import {
  useCreateTrainerMutation,
  useTrainersQuery,
  useUpdateTrainerMutation,
} from '../hooks/useTrainer';
import type { Trainer } from '../types/trainer';
import {
  EMPTY_TRAINER_FORM,
  toTrainerFormValues,
  toTrainerSaveRequest,
  trainerSchema,
} from '../types/trainerSchema';
import type { TrainerFormValues } from '../types/trainerSchema';

interface TrainerManageModalProps {
  onClose: () => void;
}

const SAVE_ERROR_FALLBACK = '트레이너 저장에 실패했습니다. 잠시 후 다시 시도해주세요.';

/**
 * 트레이너 등록·수정·비활성 처리. 트레이너는 지난 예약이 참조하므로 삭제 대신 비활성으로 둔다.
 * 한 폼을 등록과 수정이 함께 쓴다 — editingTrainer 가 있으면 수정이다.
 */
export function TrainerManageModal({ onClose }: TrainerManageModalProps) {
  const [editingTrainer, setEditingTrainer] = useState<Trainer | null>(null);
  const { data: trainers, isPending, isError } = useTrainersQuery(true);
  const createMutation = useCreateTrainerMutation();
  const updateMutation = useUpdateTrainerMutation();

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { errors },
  } = useForm<TrainerFormValues>({
    resolver: zodResolver(trainerSchema),
    defaultValues: EMPTY_TRAINER_FORM,
  });

  const phoneField = register('phone');
  const isSaving = createMutation.isPending || updateMutation.isPending;
  const saveError = createMutation.error ?? updateMutation.error;

  const resetForm = () => {
    setEditingTrainer(null);
    reset(EMPTY_TRAINER_FORM);
  };

  const handleEdit = (trainer: Trainer) => {
    setEditingTrainer(trainer);
    reset(toTrainerFormValues(trainer));
  };

  const onSubmit = (values: TrainerFormValues) => {
    const request = toTrainerSaveRequest(values);
    if (editingTrainer) {
      updateMutation.mutate({ id: editingTrainer.id, request }, { onSuccess: resetForm });
      return;
    }
    createMutation.mutate(request, { onSuccess: resetForm });
  };

  const handleToggleActive = (trainer: Trainer) => {
    updateMutation.mutate({
      id: trainer.id,
      request: { name: trainer.name, phone: trainer.phone, active: !trainer.active },
    });
  };

  return (
    <Modal title="트레이너 관리" onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-3" noValidate>
        <p className="text-sm font-semibold">
          {editingTrainer ? `${editingTrainer.name} 수정` : '새 트레이너 등록'}
        </p>
        <div className="grid gap-3 sm:grid-cols-2">
          <Input label="이름" placeholder="김코치" error={errors.name?.message} {...register('name')} />
          <Input
            label="전화번호 (선택)"
            inputMode="numeric"
            placeholder="010-1234-5678"
            error={errors.phone?.message}
            {...phoneField}
            onChange={(event) => {
              setValue('phone', formatPhone(event.target.value), { shouldValidate: true });
            }}
          />
        </div>
        {saveError && (
          <p className="text-sm text-danger">{getApiErrorMessage(saveError, SAVE_ERROR_FALLBACK)}</p>
        )}
        <div className="flex justify-end gap-2">
          {editingTrainer && (
            <Button type="button" variant="ghost" size="sm" onClick={resetForm}>
              수정 취소
            </Button>
          )}
          <Button type="submit" size="sm" isLoading={isSaving}>
            {editingTrainer ? '저장' : '등록'}
          </Button>
        </div>
      </form>

      <div className="mt-4 border-t border-border pt-3">
        <h3 className="text-sm font-semibold">트레이너 목록</h3>
        {isPending && <p className="mt-2 text-sm text-text-muted">불러오는 중...</p>}
        {isError && <p className="mt-2 text-sm text-danger">트레이너 목록을 불러오지 못했습니다.</p>}
        {trainers?.length === 0 && (
          <p className="mt-2 text-sm text-text-muted">등록된 트레이너가 없습니다.</p>
        )}
        <ul className="mt-1 max-h-64 divide-y divide-border overflow-y-auto">
          {trainers?.map((trainer) => (
            <li key={trainer.id} className="flex items-center justify-between gap-2 py-2">
              <div className="min-w-0">
                <p className={`text-sm font-medium ${trainer.active ? '' : 'text-text-muted line-through'}`}>
                  {trainer.name}
                  {!trainer.active && <span className="ml-1.5 text-xs no-underline">비활성</span>}
                </p>
                <p className="text-xs text-text-muted">{trainer.phone ?? '연락처 없음'}</p>
              </div>
              <div className="flex shrink-0 gap-1">
                <Button
                  variant="ghost"
                  size="sm"
                  aria-label={`${trainer.name} 수정`}
                  onClick={() => handleEdit(trainer)}
                >
                  수정
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  aria-label={`${trainer.name} ${trainer.active ? '비활성 처리' : '다시 활성화'}`}
                  disabled={updateMutation.isPending}
                  onClick={() => handleToggleActive(trainer)}
                >
                  {trainer.active ? '비활성' : '활성화'}
                </Button>
              </div>
            </li>
          ))}
        </ul>
        <p className="mt-2 text-xs text-text-muted">
          비활성 트레이너는 시간표 선택에서 빠지고 새 예약을 받을 수 없습니다. 지난 예약 기록은 남습니다.
        </p>
      </div>
    </Modal>
  );
}
