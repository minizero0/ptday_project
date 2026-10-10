import { API_BASE_URL, BEARER_PREFIX, TOKEN_KEY } from '../../../lib/api';

interface AttendanceEventHandlers {
  // 연결(재연결)됐을 때. 끊겨 있던 동안 놓친 출석이 있을 수 있으니 한 번 다시 불러오는 데 쓴다.
  onConnected: () => void;
  // 출석이 기록됐을 때. date 는 yyyy-MM-dd
  onCheckIn: (date: string) => void;
}

const EVENTS_PATH = '/api/attendances/events';
const CHECK_IN_EVENT_NAME = 'check-in';
const EVENT_SEPARATOR = '\n\n';
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;
const HTTP_UNAUTHORIZED = 401;
const HTTP_FORBIDDEN = 403;
// 끊기면 조금씩 간격을 늘려 가며 다시 붙는다 — 서버가 내려가 있을 때 요청을 쏟아붓지 않게
const RETRY_MIN_MS = 1_000;
const RETRY_MAX_MS = 30_000;

// 서버가 보낸 이벤트 한 덩어리("event:이름\ndata:값")에서 출석 날짜를 꺼낸다. 다른 이벤트·주석이면 null.
function parseCheckInDate(block: string): string | null {
  let eventName = '';
  let data = '';
  block.split('\n').forEach((line) => {
    if (line.startsWith('event:')) {
      eventName = line.slice('event:'.length).trim();
    } else if (line.startsWith('data:')) {
      data = line.slice('data:'.length).trim();
    }
  });
  // 외부에서 온 값이라 형식을 확인하고 쓴다
  return eventName === CHECK_IN_EVENT_NAME && DATE_PATTERN.test(data) ? data : null;
}

async function readEvents(
  body: ReadableStream<Uint8Array>,
  onCheckIn: (date: string) => void,
): Promise<void> {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  for (;;) {
    const { done, value } = await reader.read();
    if (done) {
      return;
    }
    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n');
    const blocks = buffer.split(EVENT_SEPARATOR);
    buffer = blocks.pop() ?? '';
    blocks.forEach((block) => {
      const date = parseCheckInDate(block);
      if (date) {
        onCheckIn(date);
      }
    });
  }
}

function wait(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve) => {
    const timer = setTimeout(resolve, ms);
    signal.addEventListener('abort', () => {
      clearTimeout(timer);
      resolve();
    });
  });
}

/**
 * 출석 실시간 알림(SSE)을 구독한다. 돌려주는 함수를 부르면 구독을 끝낸다.
 * 브라우저 기본 EventSource 는 Authorization 헤더를 보낼 수 없어 토큰을 주소에 실어야 한다.
 * 토큰이 주소·로그에 남지 않도록 fetch 로 직접 스트림을 읽는다.
 */
export function subscribeAttendanceEvents(handlers: AttendanceEventHandlers): () => void {
  const controller = new AbortController();
  const { signal } = controller;

  const run = async () => {
    let retryMs = RETRY_MIN_MS;

    while (!signal.aborted) {
      const token = localStorage.getItem(TOKEN_KEY);
      if (!token) {
        return;
      }
      try {
        const response = await fetch(`${API_BASE_URL}${EVENTS_PATH}`, {
          headers: { Authorization: `${BEARER_PREFIX}${token}`, Accept: 'text/event-stream' },
          signal,
        });
        if (response.status === HTTP_UNAUTHORIZED || response.status === HTTP_FORBIDDEN) {
          // 세션 만료·권한 없음은 다시 붙어도 같다. 만료 처리는 일반 요청의 공통 인터셉터가 맡는다 (lib/api).
          return;
        }
        if (response.ok && response.body) {
          retryMs = RETRY_MIN_MS;
          handlers.onConnected();
          await readEvents(response.body, handlers.onCheckIn);
        }
      } catch {
        // 네트워크 끊김·서버 재시작, 또는 구독 종료(abort). 종료가 아니면 아래에서 다시 붙는다.
      }
      await wait(retryMs, signal);
      retryMs = Math.min(retryMs * 2, RETRY_MAX_MS);
    }
  };

  void run();
  return () => controller.abort();
}
