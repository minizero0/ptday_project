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

// 예약 시작으로 고를 수 있는 분: 00 ~ 50, 10분 간격. 시(hour)는 TIMETABLE_HOURS 를 그대로 쓴다.
// 수업이 영업 종료를 넘기는 조합(예: 22시 40분 + 30분 수업)은 폼 검증이 걸러 낸다.
export const START_MINUTE_OPTIONS = Array.from(
  { length: MINUTES_PER_HOUR / START_MINUTE_STEP },
  (_, index) => index * START_MINUTE_STEP,
);

// 시·분 → 하루 중 몇 분째인지
export function toMinutes(hour: number, minute: number): number {
  return hour * MINUTES_PER_HOUR + minute;
}

// 시간표 칸을 누르지 않고 예약을 열 때의 기본 시작 시각: 지금 이후 가장 가까운 10분 단위.
// 영업 시간 밖이면 여는 시각으로 둔다 — 직원이 모달에서 고른다.
export function nextStartMinutes(now: Date): number {
  const next = (Math.floor(toMinutesOfDay(now) / START_MINUTE_STEP) + 1) * START_MINUTE_STEP;
  return next >= OPENING_MINUTES && next < CLOSING_MINUTES ? next : OPENING_MINUTES;
}

// yyyy-MM-dd → '9/21 (월)'
export function formatDayLabel(date: string): string {
  const parsed = parseDateString(date);
  return `${parsed.getMonth() + 1}/${parsed.getDate()} (${WEEKDAY_LABELS[parsed.getDay()]})`;
}
