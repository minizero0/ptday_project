import { zodResolver } from '@hookform/resolvers/zod';
import { useMemo } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { Button } from '../../../components/Button';
import { Input } from '../../../components/Input';
import { Modal } from '../../../components/Modal';
import { getApiErrorMessage } from '../../../lib/apiError';
import { cn } from '../../../lib/cn';
import type { Member } from '../../members/types/member';
import { useAdjustPtPassMutation } from '../hooks/usePtPass';
import type { PtPass } from '../types/ptPass';
import {
  ADJUST_DIRECTIONS,
  createPtPassAdjustSchema,
  toPtPassAdjustRequest,
  toSignedDelta,
} from '../types/ptPassSchema';
import type { AdjustDirection, PtPassAdjustFormValues } from '../types/ptPassSchema';
import { PtPassAdjustmentHistory } from './PtPassAdjustmentHistory';

interface PtPassAdjustModalProps {
  member: Member;
  ptPass: PtPass;
  onClose: () => void;
}

const DIRECTION_LABELS: Record<AdjustDirection, string> = { ADD: '추가', SUBTRACT: '차감' };
const ADJUST_ERROR_FALLBACK = '횟수 조정에 실패했습니다. 잠시 후 다시 시도해주세요.';

export function PtPassAdjustModal({ member, ptPass, onClose }: PtPassAdjustModalProps) {
  const schema = useMemo(
    () => createPtPassAdjustSchema(ptPass.remainingCount),
    [ptPass.remainingCount],
  );

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<PtPassAdjustFormValues>({
    resolver: zodResolver(schema),
    defaultValues: { direction: 'ADD', reason: '' },
  });

  const direction = useWatch({ control, name: 'direction' });
  const count = useWatch({ control, name: 'count' });
  const adjustMutation = useAdjustPtPassMutation(ptPass.id);

  // 저장 전에 결과를 눈으로 확인하게 한다. 실제 계산과 검증은 서버가 한다.
  const previewRemaining = Number.isInteger(count)
    ? ptPass.remainingCount + toSignedDelta(direction, count)
    : null;

  const onSubmit = (values: PtPassAdjustFormValues) => {
    adjustMutation.mutate(toPtPassAdjustRequest(values), { onSuccess: onClose });
  };

  return (
    <Modal title="PT 횟수 조정" onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <div className="flex items-center justify-between rounded-md bg-background px-3 py-2">
          <div>
            <p className="text-sm font-semibold">{member.name}</p>
            <p className="text-xs text-text-muted">{member.memberNo}</p>
          </div>
          <p className="text-sm">
            잔여 <span className="font-semibold">{ptPass.remainingCount}회</span>
            <span className="text-text-muted"> · 구매 {ptPass.totalCount}회</span>
          </p>
        </div>

        <fieldset>
          <legend className="mb-1 block text-sm font-medium">구분</legend>
          <div className="grid grid-cols-2 gap-2">
            {ADJUST_DIRECTIONS.map((option) => (
              <label
                key={option}
                className={cn(
                  // relative: 숨긴 라디오 버튼이 이 칸 안에 머물게 한다.
                  // focus-within: 라디오 버튼은 보이지 않으므로 키보드 포커스를 칸 테두리로 드러낸다.
                  'relative cursor-pointer rounded-md border px-3 py-2 text-center text-sm',
                  'focus-within:ring-2 focus-within:ring-primary',
                  direction === option
                    ? 'border-primary bg-primary/10 font-semibold'
                    : 'border-border bg-surface hover:bg-background',
                )}
              >
                <input type="radio" value={option} className="sr-only" {...register('direction')} />
                {DIRECTION_LABELS[option]}
              </label>
            ))}
          </div>
        </fieldset>

        <Input
          label="횟수"
          type="number"
          inputMode="numeric"
          min={1}
          placeholder="1"
          error={errors.count?.message}
          {...register('count', { valueAsNumber: true })}
        />

        <Input
          label="사유"
          placeholder="예: 재등록 서비스 2회 추가"
          autoComplete="off"
          error={errors.reason?.message}
          {...register('reason')}
        />

        {previewRemaining !== null && previewRemaining >= 0 && (
          <p role="status" className="text-sm text-text-muted">
            조정 후 잔여{' '}
            <span className="font-semibold text-text-primary">
              {ptPass.remainingCount}회 → {previewRemaining}회
            </span>
          </p>
        )}

        {adjustMutation.isError && (
          <p className="text-sm text-danger">
            {getApiErrorMessage(adjustMutation.error, ADJUST_ERROR_FALLBACK)}
          </p>
        )}

        <div className="flex justify-end gap-2 border-t border-border pt-4">
          <Button type="button" variant="ghost" onClick={onClose}>
            닫기
          </Button>
          <Button type="submit" isLoading={adjustMutation.isPending}>
            조정
          </Button>
        </div>
      </form>

      <div className="mt-4 border-t border-border pt-3">
        <h3 className="text-sm font-semibold">변동 이력</h3>
        <PtPassAdjustmentHistory ptPassId={ptPass.id} />
      </div>
    </Modal>
  );
}
