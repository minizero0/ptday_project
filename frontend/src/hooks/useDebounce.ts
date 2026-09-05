import { useEffect, useState } from 'react';

/** 입력 중 매 글자마다 요청이 나가지 않도록 값 변경을 지연시킨다. */
export function useDebounce<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);

  return debounced;
}
