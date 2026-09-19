import { useEffect, useState } from 'react';
import { Button } from '../../../components/Button';
import { addDays, formatDay, startOfWeek, todayString } from '../../../lib/date';
import { TrainerManageModal } from '../../trainers/components/TrainerManageModal';
import { useTrainersQuery } from '../../trainers/hooks/useTrainer';
import { PtReservationCreateModal } from '../components/PtReservationCreateModal';
import { PtReservationDetailModal } from '../components/PtReservationDetailModal';
import { WeekTimetable } from '../components/WeekTimetable';
import { usePtReservationsQuery } from '../hooks/usePtReservation';
import type { PtReservation } from '../types/ptReservation';
import { DAYS_PER_WEEK } from '../types/timetable';

interface SlotSelection {
  date: string;
  minutesOfDay: number;
}

const NOW_REFRESH_MS = 60_000;

// 현재 시각 표시선과 "지난 칸" 판단이 화면을 켜 둔 동안에도 따라오게 1분마다 갱신한다
function useNow(): Date {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), NOW_REFRESH_MS);
    return () => clearInterval(timer);
  }, []);

  return now;
}

export function PtReservationsPage() {
  const [weekStart, setWeekStart] = useState(() => startOfWeek(todayString()));
  const [pickedTrainerId, setPickedTrainerId] = useState<number | null>(null);
  const [slotSelection, setSlotSelection] = useState<SlotSelection | null>(null);
  const [selectedReservationId, setSelectedReservationId] = useState<number | null>(null);
  const [isTrainerManageOpen, setIsTrainerManageOpen] = useState(false);
  const now = useNow();

  const trainersQuery = useTrainersQuery();
  const trainers = trainersQuery.data ?? [];
  // 직접 고르기 전에는 첫 번째 트레이너. 고른 트레이너가 비활성 처리돼 목록에서 빠져도 첫 번째로 돌아간다.
  const trainer = trainers.find((candidate) => candidate.id === pickedTrainerId) ?? trainers[0] ?? null;

  const weekEnd = addDays(weekStart, DAYS_PER_WEEK - 1);
  const reservationsQuery = usePtReservationsQuery(
    trainer ? { trainerId: trainer.id, from: weekStart, to: weekEnd } : null,
  );
  const isThisWeek = weekStart === startOfWeek(todayString());
  // 상세 모달은 목록에서 다시 찾은 최신 값을 본다. 다른 직원이 먼저 취소해 목록에서 빠지면 모달도 닫힌다.
  const selectedReservation =
    reservationsQuery.data?.find((reservation) => reservation.id === selectedReservationId) ?? null;

  return (
    <div className="flex h-full flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold">PT 예약</h1>
          <p className="mt-1 text-sm text-text-muted">
            {formatDay(weekStart)} ~ {formatDay(weekEnd)} · 예약{' '}
            <span className="font-semibold text-text-primary">
              {reservationsQuery.data?.length ?? '-'}건
            </span>
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <label htmlFor="pt-reservation-trainer" className="sr-only">
            트레이너
          </label>
          <select
            id="pt-reservation-trainer"
            value={trainer?.id ?? ''}
            disabled={trainers.length === 0}
            onChange={(event) => setPickedTrainerId(Number(event.target.value))}
            className="rounded-md border border-border bg-surface px-3 py-1.5 text-sm"
          >
            {trainers.length === 0 && <option value="">트레이너 없음</option>}
            {trainers.map((candidate) => (
              <option key={candidate.id} value={candidate.id}>
                {candidate.name}
              </option>
            ))}
          </select>
          <Button variant="ghost" size="sm" onClick={() => setIsTrainerManageOpen(true)}>
            트레이너 관리
          </Button>

          <Button
            variant="ghost"
            size="sm"
            aria-label="이전 주"
            onClick={() => setWeekStart(addDays(weekStart, -DAYS_PER_WEEK))}
          >
            ◀
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={isThisWeek}
            onClick={() => setWeekStart(startOfWeek(todayString()))}
          >
            이번 주
          </Button>
          <Button
            variant="ghost"
            size="sm"
            aria-label="다음 주"
            onClick={() => setWeekStart(addDays(weekStart, DAYS_PER_WEEK))}
          >
            ▶
          </Button>
        </div>
      </div>

      {/* 공용 Card 는 안쪽 여백이 있어, 스크롤할 때 고정 헤더 위로 내용이 비친다 — 여백 없는 같은 모양의 틀을 쓴다 */}
      <div className="min-h-0 flex-1 overflow-auto rounded-xl border border-border bg-surface shadow-card">
        {trainersQuery.isPending && (
          <p className="px-4 py-10 text-center text-sm text-text-muted">불러오는 중...</p>
        )}
        {trainersQuery.isError && (
          <p className="px-4 py-10 text-center text-sm text-danger">
            트레이너 목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.
          </p>
        )}
        {trainersQuery.isSuccess && !trainer && (
          <div className="px-4 py-10 text-center">
            <p className="text-sm text-text-muted">
              등록된 트레이너가 없습니다. 트레이너를 먼저 등록하면 시간표가 열립니다.
            </p>
            <Button className="mt-3" size="sm" onClick={() => setIsTrainerManageOpen(true)}>
              트레이너 등록
            </Button>
          </div>
        )}
        {trainer && reservationsQuery.isError && (
          <p className="px-4 py-10 text-center text-sm text-danger">
            예약을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.
          </p>
        )}
        {trainer && reservationsQuery.isPending && (
          <p className="px-4 py-10 text-center text-sm text-text-muted">예약을 불러오는 중...</p>
        )}
        {trainer && reservationsQuery.isSuccess && (
          <WeekTimetable
            weekStart={weekStart}
            reservations={reservationsQuery.data}
            now={now}
            onSlotClick={(date, minutesOfDay) => setSlotSelection({ date, minutesOfDay })}
            onReservationClick={(reservation: PtReservation) => setSelectedReservationId(reservation.id)}
          />
        )}
      </div>

      {slotSelection && trainer && (
        <PtReservationCreateModal
          trainer={trainer}
          initialDate={slotSelection.date}
          initialMinutes={slotSelection.minutesOfDay}
          onClose={() => setSlotSelection(null)}
        />
      )}
      {selectedReservation && (
        <PtReservationDetailModal
          reservation={selectedReservation}
          onClose={() => setSelectedReservationId(null)}
        />
      )}
      {isTrainerManageOpen && <TrainerManageModal onClose={() => setIsTrainerManageOpen(false)} />}
    </div>
  );
}
