import { zodResolver } from '@hookform/resolvers/zod';
import { Controller, useForm } from 'react-hook-form';
import { Button } from '../../../components/Button';
import { Input } from '../../../components/Input';
import { Modal } from '../../../components/Modal';
import { getApiErrorMessage } from '../../../lib/apiError';
import type { Member } from '../../members/types/member';
import { useGrantPtPassMutation } from '../hooks/usePtPass';
import { SESSION_MINUTES_OPTIONS } from '../types/ptPass';
import { ptPassCreateSchema } from '../types/ptPassSchema';
import type { PtPassCreateFormValues } from '../types/ptPassSchema';

interface PtPassCreateModalProps {
  member: Member;
  onClose: () => void;
}

// 자주 파는 횟수. 서버 제약이 아니라 입력을 줄여 주는 화면 편의 값이다.
const PT_COUNT_PRESETS = [10, 20, 30, 50] as const;
const GRANT_ERROR_FALLBACK = 'PT권 등록에 실패했습니다. 잠시 후 다시 시도해주세요.';

export function PtPassCreateModal({ member, onClose }: PtPassCreateModalProps) {
  const {
    register,
    control,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<PtPassCreateFormValues>({ resolver: zodResolver(ptPassCreateSchema) });

  const grantMutation = useGrantPtPassMutation(member.id);

  const onSubmit = (values: PtPassCreateFormValues) => {
    grantMutation.mutate(values, { onSuccess: onClose });
  };

  return (
    <Modal title="PT권 등록" onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <div className="rounded-md bg-background px-3 py-2">
          <p className="text-sm font-semibold">{member.name}</p>
          <p className="text-xs text-text-muted">{member.memberNo}</p>
        </div>

        <Controller
          control={control}
          name="sessionMinutes"
          render={({ field }) => (
            <fieldset>
              <legend className="mb-1 text-sm font-medium">수업 길이</legend>
              <div className="grid grid-cols-4 gap-2">
                {SESSION_MINUTES_OPTIONS.map((minutes) => (
                  <label
                    key={minutes}
                    className={`relative cursor-pointer rounded-md border px-3 py-2 text-center text-sm focus-within:ring-2 focus-within:ring-primary ${
                      field.value === minutes
                        ? 'border-primary bg-primary/10 font-semibold text-primary'
                        : 'border-border bg-surface hover:bg-background'
                    }`}
                  >
                    <input
                      type="radio"
                      name={field.name}
                      className="sr-only"
                      checked={field.value === minutes}
                      onChange={() => field.onChange(minutes)}
                      onBlur={field.onBlur}
                    />
                    {minutes}분
                  </label>
                ))}
              </div>
              {errors.sessionMinutes && (
                <p className="mt-1 text-sm text-danger">{errors.sessionMinutes.message}</p>
              )}
            </fieldset>
          )}
        />

        <Input
          label="PT 횟수"
          type="number"
          inputMode="numeric"
          min={1}
          placeholder="10"
          error={errors.totalCount?.message}
          {...register('totalCount', { valueAsNumber: true })}
        />

        <div className="flex flex-wrap gap-2">
          {PT_COUNT_PRESETS.map((count) => (
            <button
              key={count}
              type="button"
              onClick={() => setValue('totalCount', count, { shouldValidate: true })}
              className="rounded-md border border-border bg-surface px-3 py-1.5 text-sm hover:bg-background"
            >
              {count}회
            </button>
          ))}
        </div>

        <p className="text-xs text-text-muted">
          등록하면 잔여 횟수가 같은 값으로 시작합니다. 예약은 이 수업 길이로만 잡히고, 유효 기간은 없습니다.
        </p>

        {grantMutation.isError && (
          <p className="text-sm text-danger">
            {getApiErrorMessage(grantMutation.error, GRANT_ERROR_FALLBACK)}
          </p>
        )}

        <div className="flex justify-end gap-2 border-t border-border pt-4">
          <Button type="button" variant="ghost" onClick={onClose}>
            취소
          </Button>
          <Button type="submit" isLoading={grantMutation.isPending}>
            등록
          </Button>
        </div>
      </form>
    </Modal>
  );
}
