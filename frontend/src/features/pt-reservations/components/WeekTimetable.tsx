import type { MouseEvent } from 'react';
import { addDays, toDateString, todayString } from '../../../lib/date';
import { cn } from '../../../lib/cn';
import type { PtReservation } from '../types/ptReservation';
import {
  DAYS_PER_WEEK,
  OPENING_MINUTES,
  START_MINUTE_STEP,
  TIMETABLE_HOURS,
  formatDayLabel,
  formatMinutes,
  toMinutesOfDay,
} from '../types/timetable';

interface WeekTimetableProps {
  weekStart: string; // 월요일, yyyy-MM-dd
  reservations: PtReservation[];
  now: Date;
  onSlotClick: (date: string, minutesOfDay: number) => void;
  onReservationClick: (reservation: PtReservation) => void;
}

// 1분 = 1.2px → 10분 한 칸 12px, 1시간 72px. 30분 수업도 이름과 시각이 두 줄로 들어간다.
const PIXELS_PER_MINUTE = 1.2;
const MINUTES_PER_HOUR = 60;
const HOUR_HEIGHT = MINUTES_PER_HOUR * PIXELS_PER_MINUTE;

// 그 시(hour)에 걸쳐 있는 예약 수. 칸 버튼의 안내(aria-label)에 쓴다 — 예약 블록은 화면에만 겹쳐 보이기 때문이다.
function countReservationsInHour(reservations: PtReservation[], hour: number): number {
  const hourStart = hour * MINUTES_PER_HOUR;
  const hourEnd = hourStart + MINUTES_PER_HOUR;
  return reservations.filter((reservation) => {
    const startMinutes = toMinutesOfDay(new Date(reservation.startTime));
    return startMinutes < hourEnd && startMinutes + reservation.sessionMinutes > hourStart;
  }).length;
}

function groupByDay(reservations: PtReservation[]): Map<string, PtReservation[]> {
  const byDay = new Map<string, PtReservation[]>();
  reservations.forEach((reservation) => {
    const day = toDateString(new Date(reservation.startTime));
    byDay.set(day, [...(byDay.get(day) ?? []), reservation]);
  });
  return byDay;
}

/**
 * 트레이너 한 명의 주간 시간표. 빈 칸(1시간 단위 버튼)을 누르면 누른 높이를 10분 단위로 읽어 예약 등록을 연다.
 * 키보드로 누르면 그 시간의 정각이다 — 정확한 분은 등록 모달에서 고친다.
 */
export function WeekTimetable({
  weekStart,
  reservations,
  now,
  onSlotClick,
  onReservationClick,
}: WeekTimetableProps) {
  const days = Array.from({ length: DAYS_PER_WEEK }, (_, index) => addDays(weekStart, index));
  const reservationsByDay = groupByDay(reservations);
  const today = todayString();
  const nowMinutes = toMinutesOfDay(now);

  const handleHourClick = (event: MouseEvent<HTMLButtonElement>, date: string, hour: number) => {
    const { top } = event.currentTarget.getBoundingClientRect();
    // 키보드(Enter/Space)로 누르면 clientY 가 0 이라 음수가 나온다 → 정각으로 본다
    const offsetMinutes = Math.max(0, (event.clientY - top) / PIXELS_PER_MINUTE);
    const snappedMinutes = Math.min(
      MINUTES_PER_HOUR - START_MINUTE_STEP,
      Math.floor(offsetMinutes / START_MINUTE_STEP) * START_MINUTE_STEP,
    );
    onSlotClick(date, hour * MINUTES_PER_HOUR + (event.detail === 0 ? 0 : snappedMinutes));
  };

  return (
    <div className="min-w-[860px]">
      <div className="sticky top-0 z-20 grid grid-cols-[3.5rem_repeat(7,minmax(0,1fr))] border-b border-border bg-surface">
        <div className="sticky left-0 z-10 bg-surface" />
        {days.map((date) => (
          <div
            key={date}
            className={cn(
              'border-l border-border px-2 py-2 text-center text-sm font-medium',
              date === today && 'bg-primary/10 font-bold',
            )}
          >
            {formatDayLabel(date)}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-[3.5rem_repeat(7,minmax(0,1fr))]">
        {/* 좁은 화면에서 가로로 밀어도 시각은 보이게 왼쪽에 고정한다. 예약 블록(z-10)보다 위에 둔다 */}
        <div aria-hidden="true" className="sticky left-0 z-[15] bg-surface">
          {TIMETABLE_HOURS.map((hour) => (
            <div
              key={hour}
              style={{ height: HOUR_HEIGHT }}
              className="border-b border-border pr-2 pt-1 text-right text-xs text-text-muted"
            >
              {formatMinutes(hour * MINUTES_PER_HOUR)}
            </div>
          ))}
        </div>

        {days.map((date) => {
          const isPastDay = date < today;
          const isToday = date === today;
          const dayReservations = reservationsByDay.get(date) ?? [];

          return (
            <div key={date} className="relative border-l border-border">
              {TIMETABLE_HOURS.map((hour) => {
                const isPastHour = isPastDay || (isToday && (hour + 1) * MINUTES_PER_HOUR <= nowMinutes);
                const reservedCount = countReservationsInHour(dayReservations, hour);
                const occupancyLabel = reservedCount > 0 ? ` (이 시간에 예약 ${reservedCount}건 있음)` : '';
                return (
                  <button
                    key={hour}
                    type="button"
                    disabled={isPastHour}
                    aria-label={`${formatDayLabel(date)} ${hour}시 예약 추가${occupancyLabel}`}
                    style={{ height: HOUR_HEIGHT }}
                    onClick={(event) => handleHourClick(event, date, hour)}
                    className={cn(
                      'block w-full border-b border-border focus-visible:relative focus-visible:z-10',
                      isPastHour ? 'cursor-not-allowed bg-background/60' : 'hover:bg-primary/5',
                    )}
                  />
                );
              })}

              {isToday && nowMinutes >= OPENING_MINUTES && (
                <div
                  aria-hidden="true"
                  style={{ top: (nowMinutes - OPENING_MINUTES) * PIXELS_PER_MINUTE }}
                  className="pointer-events-none absolute inset-x-0 z-10 border-t-2 border-danger"
                />
              )}

              {dayReservations.map((reservation) => {
                const startMinutes = toMinutesOfDay(new Date(reservation.startTime));
                const timeRange = `${formatMinutes(startMinutes)}~${formatMinutes(startMinutes + reservation.sessionMinutes)}`;
                return (
                  <button
                    key={reservation.id}
                    type="button"
                    aria-label={`${formatDayLabel(date)} ${timeRange} ${reservation.memberName} 예약 상세`}
                    onClick={() => onReservationClick(reservation)}
                    style={{
                      top: (startMinutes - OPENING_MINUTES) * PIXELS_PER_MINUTE,
                      height: reservation.sessionMinutes * PIXELS_PER_MINUTE,
                    }}
                    className="absolute inset-x-1 z-10 overflow-hidden rounded-md border-l-4 border-primary bg-primary/20 px-1.5 py-0.5 text-left shadow-card hover:bg-primary/30"
                  >
                    <span className="block truncate text-xs font-semibold leading-tight">
                      {reservation.memberName}
                    </span>
                    <span className="block truncate text-[11px] leading-tight text-text-muted">
                      {timeRange}
                    </span>
                  </button>
                );
              })}
            </div>
          );
        })}
      </div>
    </div>
  );
}
