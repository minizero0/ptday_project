import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { Button } from '../../../components/Button';
import { Input } from '../../../components/Input';
import { Modal } from '../../../components/Modal';
import { getApiErrorMessage } from '../../../lib/apiError';
import { MemberPicker } from '../../members/components/MemberPicker';
import type { Member } from '../../members/types/member';
import { usePtPassesQuery } from '../../pt-passes/hooks/usePtPass';
import { SESSION_MINUTES_OPTIONS } from '../../pt-passes/types/ptPass';
import type { PtPass, SessionMinutes } from '../../pt-passes/types/ptPass';
import type { Trainer } from '../../trainers/types/trainer';
import { useCreatePtReservationMutation } from '../hooks/usePtReservation';
import { ptReservationCreateSchema } from '../types/ptReservationSchema';
import type { PtReservationCreateFormValues } from '../types/ptReservationSchema';
import {
  START_MINUTE_OPTIONS,
  TIMETABLE_HOURS,
  formatMinutes,
  toInstant,
  toMinutes,
} from '../types/timetable';

interface PtReservationCreateModalProps {
  trainer: Trainer;
  initialDate: string; // yyyy-MM-dd
  initialMinutes: number; // 하루 중 몇 분째인지
  onClose: () => void;
}

interface SessionOption {
  sessionMinutes: SessionMinutes;
  remainingCount: number;
}

const MINUTES_PER_HOUR = 60;
const CREATE_ERROR_FALLBACK = 'PT 예약에 실패했습니다. 잠시 후 다시 시도해주세요.';

// 회원이 가진 PT권을 수업 길이별로 묶는다. 잔여가 남은 길이만 예약할 수 있다.
function toSessionOptions(ptPasses: PtPass[]): SessionOption[] {
  return SESSION_MINUTES_OPTIONS.map((sessionMinutes) => ({
    sessionMinutes,
    remainingCount: ptPasses
      .filter((ptPass) => ptPass.sessionMinutes === sessionMinutes)
      .reduce((sum, ptPass) => sum + ptPass.remainingCount, 0),
  })).filter((option) => option.remainingCount > 0);
}

export function PtReservationCreateModal({
  trainer,
  initialDate,
  initialMinutes,
  onClose,
}: PtReservationCreateModalProps) {
  const [member, setMember] = useState<Member | null>(null);
  const { data: ptPasses, isPending: isPtPassesPending, isError: isPtPassesError } = usePtPassesQuery(
    member?.id ?? null,
  );
  const createMutation = useCreatePtReservationMutation();

  const {
    register,
    control,
    handleSubmit,
    resetField,
    formState: { errors },
  } = useForm<PtReservationCreateFormValues>({
    resolver: zodResolver(ptReservationCreateSchema),
    defaultValues: {
      date: initialDate,
      hour: Math.floor(initialMinutes / MINUTES_PER_HOUR),
      minute: initialMinutes % MINUTES_PER_HOUR,
    },
  });

  const [hour, minute, sessionMinutes] = useWatch({
    control,
    name: ['hour', 'minute', 'sessionMinutes'],
  });
  const sessionOptions = ptPasses ? toSessionOptions(ptPasses) : [];
  const startMinutes = toMinutes(hour, minute);
  const timeError = errors.hour?.message ?? errors.minute?.message;
  const timeSelectClassName = `w-full rounded-md border bg-surface px-3 py-2 text-sm ${
    timeError ? 'border-danger' : 'border-border'
  }`;

  const handleMemberChange = (next: Member | null) => {
    setMember(next);
    // 회원마다 가진 수업 길이가 다르다 — 이전 회원 기준의 선택을 남기지 않는다
    resetField('sessionMinutes');
    createMutation.reset();
  };

  const onSubmit = (values: PtReservationCreateFormValues) => {
    if (!member) {
      return;
    }
    createMutation.mutate(
      {
        memberId: member.id,
        trainerId: trainer.id,
        sessionMinutes: values.sessionMinutes,
        startTime: toInstant(values.date, toMinutes(values.hour, values.minute)),
      },
      { onSuccess: onClose },
    );
  };

  return (
    <Modal title="PT 예약 등록" onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <div className="rounded-md bg-background px-3 py-2">
          <p className="text-xs text-text-muted">트레이너</p>
          <p className="text-sm font-semibold">{trainer.name}</p>
        </div>

        <Input label="날짜" type="date" error={errors.date?.message} {...register('date')} />

        <fieldset>
          <legend className="mb-1 text-sm font-medium">시작 시각</legend>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label htmlFor="pt-reservation-hour" className="sr-only">
                시작 시간
              </label>
              <select
                id="pt-reservation-hour"
                aria-invalid={timeError ? true : undefined}
                className={timeSelectClassName}
                {...register('hour', { valueAsNumber: true })}
              >
                {TIMETABLE_HOURS.map((option) => (
                  <option key={option} value={option}>
                    {String(option).padStart(2, '0')}시
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="pt-reservation-minute" className="sr-only">
                시작 분
              </label>
              <select
                id="pt-reservation-minute"
                aria-invalid={timeError ? true : undefined}
                className={timeSelectClassName}
                {...register('minute', { valueAsNumber: true })}
              >
                {START_MINUTE_OPTIONS.map((option) => (
                  <option key={option} value={option}>
                    {String(option).padStart(2, '0')}분
                  </option>
                ))}
              </select>
            </div>
          </div>
          {timeError && <p className="mt-1 text-sm text-danger">{timeError}</p>}
        </fieldset>

        {member ? (
          <div className="flex items-center justify-between rounded-md bg-background px-3 py-2">
            <div>
              <p className="text-sm font-semibold">{member.name}</p>
              <p className="text-xs text-text-muted">
                {member.memberNo} · {member.phone ?? '연락처 없음'}
              </p>
            </div>
            <Button type="button" variant="ghost" size="sm" onClick={() => handleMemberChange(null)}>
              변경
            </Button>
          </div>
        ) : (
          <MemberPicker onSelect={handleMemberChange} />
        )}

        {member && (
          <Controller
            control={control}
            name="sessionMinutes"
            render={({ field }) => (
              <fieldset>
                <legend className="mb-1 text-sm font-medium">수업 길이 (보유 PT권)</legend>
                {isPtPassesPending && <p className="text-sm text-text-muted">PT권을 불러오는 중...</p>}
                {isPtPassesError && (
                  <p className="text-sm text-danger">PT권을 불러오지 못했습니다.</p>
                )}
                {ptPasses && sessionOptions.length === 0 && (
                  <p className="rounded-md bg-danger/10 px-3 py-2 text-sm text-danger">
                    잔여 횟수가 남은 PT권이 없습니다. 회원관리에서 PT권을 먼저 등록하세요.
                  </p>
                )}
                <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                  {sessionOptions.map((option) => (
                    <label
                      key={option.sessionMinutes}
                      className={`relative cursor-pointer rounded-md border px-3 py-2 text-center text-sm focus-within:ring-2 focus-within:ring-primary ${
                        field.value === option.sessionMinutes
                          ? 'border-primary bg-primary/10 font-semibold'
                          : 'border-border bg-surface hover:bg-background'
                      }`}
                    >
                      <input
                        type="radio"
                        name={field.name}
                        className="sr-only"
                        checked={field.value === option.sessionMinutes}
                        onChange={() => field.onChange(option.sessionMinutes)}
                        onBlur={field.onBlur}
                      />
                      {option.sessionMinutes}분
                      <span className="block text-xs font-normal text-text-muted">
                        잔여 {option.remainingCount}회
                      </span>
                    </label>
                  ))}
                </div>
                {errors.sessionMinutes && (
                  <p className="mt-1 text-sm text-danger">{errors.sessionMinutes.message}</p>
                )}
              </fieldset>
            )}
          />
        )}

        {sessionMinutes && (
          <p className="rounded-md bg-background px-3 py-2 text-sm">
            <span className="text-text-muted">예약 시간 </span>
            <span className="font-semibold">
              {formatMinutes(startMinutes)} ~ {formatMinutes(startMinutes + sessionMinutes)}
            </span>
            <span className="text-text-muted"> · 등록하면 PT 1회가 차감됩니다 (먼저 산 PT권부터)</span>
          </p>
        )}

        {createMutation.isError && (
          <p className="text-sm text-danger">
            {getApiErrorMessage(createMutation.error, CREATE_ERROR_FALLBACK)}
          </p>
        )}

        <div className="flex justify-end gap-2 border-t border-border pt-4">
          <Button type="button" variant="ghost" onClick={onClose}>
            닫기
          </Button>
          <Button type="submit" disabled={!member} isLoading={createMutation.isPending}>
            예약
          </Button>
        </div>
      </form>
    </Modal>
  );
}
