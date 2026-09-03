// 조건부 className 을 안전하게 합치는 작은 헬퍼. falsy 값은 무시한다.
export function cn(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(' ');
}
