import { useState } from 'react';
import { Button } from '../../../components/Button';
import { Modal } from '../../../components/Modal';
import { getApiErrorMessage } from '../../../lib/apiError';
import { formatInstantTime, toDateString } from '../../../lib/date';
import { useCancelPtReservationMutation } from '../hooks/usePtReservation';
import type { PtReservation } from '../types/ptReservation';
import { formatDayLabel } from '../types/timetable';

interface PtReservationDetailModalProps {
  reservation: PtReservation;
  onClose: () => void;
}

const CANCEL_ERROR_FALLBACK = '예약 취소에 실패했습니다. 잠시 후 다시 시도해주세요.';

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4 py-2">
      <dt className="shrink-0 text-sm text-text-muted">{label}</dt>
      <dd className="text-right text-sm">{value}</dd>
    </div>
  );
}

/** 예약 상세와 취소. 취소는 되돌릴 수 없어(다시 예약해야 한다) 한 번 더 확인받는다. */
export function PtReservationDetailModal({ reservation, onClose }: PtReservationDetailModalProps) {
  const [isConfirmingCancel, setIsConfirmingCancel] = useState(false);
  const cancelMutation = useCancelPtReservationMutation();

  const dayLabel = formatDayLabel(toDateString(new Date(reservation.startTime)));
  const timeRange = `${formatInstantTime(reservation.startTime)} ~ ${formatInstantTime(reservation.endTime)}`;

  const handleCancel = () => {
    cancelMutation.mutate(reservation.id, { onSuccess: onClose });
  };

  return (
    <Modal title="PT 예약 상세" onClose={onClose}>
      <div className="rounded-md bg-background p-3">
        <p className="font-bold">{reservation.memberName}</p>
        <p className="mt-0.5 text-sm text-text-muted">{reservation.memberNo}</p>
      </div>

      <dl className="mt-2 divide-y divide-border">
        <DetailRow label="트레이너" value={reservation.trainerName} />
        <DetailRow label="일시" value={`${dayLabel} ${timeRange}`} />
        <DetailRow label="수업 길이" value={`${reservation.sessionMinutes}분`} />
        <DetailRow label="등록" value={reservation.createdBy} />
      </dl>

      {cancelMutation.isError && (
        <p className="mt-2 text-sm text-danger">
          {getApiErrorMessage(cancelMutation.error, CANCEL_ERROR_FALLBACK)}
        </p>
      )}

      {isConfirmingCancel ? (
        <div className="mt-4 rounded-md border border-danger/40 bg-danger/10 p-3" role="alert">
          <p className="text-sm">
            이 예약을 취소할까요? 차감했던 <span className="font-semibold">PT 1회가 복원</span>됩니다.
          </p>
          <div className="mt-3 flex justify-end gap-2">
            <Button
              variant="ghost"
              size="sm"
              disabled={cancelMutation.isPending}
              onClick={() => setIsConfirmingCancel(false)}
            >
              아니요
            </Button>
            <Button variant="danger" size="sm" isLoading={cancelMutation.isPending} onClick={handleCancel}>
              예약 취소
            </Button>
          </div>
        </div>
      ) : (
        <div className="mt-4 flex justify-end gap-2 border-t border-border pt-4">
          <Button variant="danger" onClick={() => setIsConfirmingCancel(true)}>
            예약 취소
          </Button>
          <Button variant="ghost" onClick={onClose}>
            닫기
          </Button>
        </div>
      )}
    </Modal>
  );
}
