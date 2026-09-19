// yyyy-MM-dd(LocalDate) → yyyy.MM.dd.
// 날짜만 있는 값은 시각대 변환 대상이 아니므로 Date 로 파싱하지 않는다 (파싱하면 하루 밀릴 수 있다).
export function formatDay(date: string): string {
  return date.replaceAll('-', '.');
}

// yyyy-MM-dd 에 개월 수를 더한다. 서버의 LocalDate.plusMonths 와 같은 말일 보정 규칙:
// 1/31 + 1개월 = 2/28. 등록 화면에서 만료일을 미리 채워 주는 용도이며, 최종 검증은 서버가 한다.
export function addMonths(date: string, months: number): string {
  const [year, month, day] = date.split('-').map(Number);
  const monthIndex = month - 1 + months;
  const targetYear = year + Math.floor(monthIndex / 12);
  const targetMonth = ((monthIndex % 12) + 12) % 12;
  const lastDayOfTargetMonth = new Date(Date.UTC(targetYear, targetMonth + 1, 0)).getUTCDate();
  const targetDay = Math.min(day, lastDayOfTargetMonth);

  return [
    String(targetYear).padStart(4, '0'),
    String(targetMonth + 1).padStart(2, '0'),
    String(targetDay).padStart(2, '0'),
  ].join('-');
}

// 오늘 날짜를 yyyy-MM-dd 로. 폼 기본값용 (기기 로컬 기준)
export function todayString(): string {
  const now = new Date();
  return [
    String(now.getFullYear()).padStart(4, '0'),
    String(now.getMonth() + 1).padStart(2, '0'),
    String(now.getDate()).padStart(2, '0'),
  ].join('-');
}

// ISO-8601 UTC 시각 → 기기 로컬 날짜 (yyyy. MM. dd.). 시각 값은 표시할 때만 로컬로 바꾼다 (CLAUDE.md §5)
export function formatInstantDay(instant: string): string {
  return new Date(instant).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}
