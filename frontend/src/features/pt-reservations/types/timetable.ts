import { parseDateString } from '../../../lib/date';

// 서버(PtReservation.OPENING_TIME / CLOSING_TIME / START_MINUTE_STEP)와 같은 값
export const OPENING_HOUR = 6;
export const CLOSING_HOUR = 23;
export const START_MINUTE_STEP = 10;

const MINUTES_PER_HOUR = 60;
export const OPENING_MINUTES = OPENING_HOUR * MINUTES_PER_HOUR;
export const CLOSING_MINUTES = CLOSING_HOUR * MINUTES_PER_HOUR;

// 시간표에 그리는 시(hour) 목록: 06 ~ 22
export const TIMETABLE_HOURS = Array.from(
  { length: CLOSING_HOUR - OPENING_HOUR },
  (_, index) => OPENING_HOUR + index,
);

export const WEEKDAY_LABELS = ['일', '월', '화', '수', '목', '금', '토'] as const;
export const DAYS_PER_WEEK = 7;

// 하루 중 몇 분째인지(0~1439) → 'HH:mm'
export function formatMinutes(minutesOfDay: number): string {
  const hour = Math.floor(minutesOfDay / MINUTES_PER_HOUR);
  const minute = minutesOfDay % MINUTES_PER_HOUR;
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
}

// 'HH:mm' → 하루 중 몇 분째인지
export function parseMinutes(time: string): number {
  const [hour, minute] = time.split(':').map(Number);
  return hour * MINUTES_PER_HOUR + minute;
}

// 시각(ISO 또는 Date) → 기기 로컬 기준 하루 중 몇 분째인지
export function toMinutesOfDay(date: Date): number {
  return date.getHours() * MINUTES_PER_HOUR + date.getMinutes();
}

// 로컬 날짜 + 하루 중 분 → 서버로 보낼 ISO-8601 UTC 시각 (CLAUDE.md §5)
export function toInstant(date: string, minutesOfDay: number): string {
  const target = parseDateString(date);
  target.setMinutes(minutesOfDay);
  return target.toISOString();
}

// 예약 시작으로 고를 수 있는 시각: 06:00 ~ 22:50, 10분 간격.
// 가장 짧은 수업도 영업 종료를 넘기는 시각(22:40 이후)은 폼 검증이 걸러 낸다.
export const START_TIME_OPTIONS = Array.from(
  { length: (CLOSING_MINUTES - OPENING_MINUTES) / START_MINUTE_STEP },
  (_, index) => formatMinutes(OPENING_MINUTES + index * START_MINUTE_STEP),
);

// yyyy-MM-dd → '9/21 (월)'
export function formatDayLabel(date: string): string {
  const parsed = parseDateString(date);
  return `${parsed.getMonth() + 1}/${parsed.getDate()} (${WEEKDAY_LABELS[parsed.getDay()]})`;
}
