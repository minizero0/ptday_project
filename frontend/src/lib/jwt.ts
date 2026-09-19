// 백엔드 JwtProvider 가 발급하는 액세스 토큰의 페이로드 (subject = 로그인 아이디, role = 권한)
export interface JwtPayload {
  sub: string;
  role: string;
  exp: number; // 만료 시각 (epoch seconds)
}

const JWT_PART_COUNT = 3;
const MILLIS_PER_SECOND = 1000;

// base64url → UTF-8 문자열. atob 은 Latin-1 만 돌려주므로 한글 아이디가 깨지지 않게 바이트로 풀어 디코딩한다.
function decodeBase64Url(segment: string): string {
  const base64 = segment.replace(/-/g, '+').replace(/_/g, '/');
  const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
  const bytes = Uint8Array.from(atob(padded), (char) => char.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}

function isJwtPayload(value: unknown): value is JwtPayload {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const candidate = value as Record<string, unknown>;
  return (
    typeof candidate.sub === 'string' &&
    typeof candidate.role === 'string' &&
    typeof candidate.exp === 'number'
  );
}

/**
 * 토큰의 페이로드를 읽는다. 서명 검증이 아니라 화면 표시용 해석일 뿐이다 —
 * 비밀키는 서버에만 있고, 권한의 최종 판단은 언제나 서버가 한다 (CLAUDE.md §12).
 * 형식이 맞지 않으면 null.
 */
export function decodeJwtPayload(token: string): JwtPayload | null {
  const parts = token.split('.');
  if (parts.length !== JWT_PART_COUNT) {
    return null;
  }
  try {
    const payload: unknown = JSON.parse(decodeBase64Url(parts[1]));
    return isJwtPayload(payload) ? payload : null;
  } catch {
    // 외부에서 온 문자열이라 깨져 있을 수 있다. 호출부가 null 을 "토큰 없음"과 같게 다룬다.
    return null;
  }
}

export function isJwtExpired(payload: JwtPayload): boolean {
  return payload.exp * MILLIS_PER_SECOND <= Date.now();
}
