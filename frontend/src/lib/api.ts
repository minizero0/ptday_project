import axios from 'axios';

export const TOKEN_KEY = 'accessToken';

// 기본은 상대경로('') — /api 요청이 Vite 프록시(개발)·nginx(운영)를 거쳐 백엔드로 간다.
// 필요 시 .env 의 VITE_API_BASE_URL 로 절대주소를 덮어쓸 수 있다.
const baseURL = import.meta.env.VITE_API_BASE_URL ?? '';

const api = axios.create({ baseURL });

const BEARER_PREFIX = 'Bearer ';
const HTTP_UNAUTHORIZED = 401;

type SessionExpiredHandler = () => void;

let sessionExpiredHandler: SessionExpiredHandler | null = null;

/**
 * 세션이 만료됐을 때 부를 함수를 등록한다 (앱 시작 시 한 번).
 * 로그인 상태 저장소가 이 모듈을 가져다 쓰므로, 여기서 저장소를 직접 부르면 순환 참조가 된다.
 * 그래서 이 모듈은 "누가 처리하는지" 모른 채 등록된 함수만 부른다.
 */
export function setSessionExpiredHandler(handler: SessionExpiredHandler): void {
  sessionExpiredHandler = handler;
}

// 요청 인터셉터: 저장된 토큰이 있으면 Authorization 헤더에 자동 첨부
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `${BEARER_PREFIX}${token}`;
  }
  return config;
});

/**
 * 이 401 이 "지금 세션의 만료"인지 판단한다.
 * - 토큰 없이 보낸 요청의 401 은 만료가 아니다 (아이디·비밀번호가 틀린 로그인 실패도 401 이다).
 * - 보낸 토큰이 지금 저장된 토큰과 다르면 이미 지나간 세션의 늦은 응답이다.
 *   다시 로그인한 직후에 도착해도 새 세션을 끊지 않도록 무시한다.
 */
function isCurrentSessionExpired(error: unknown): boolean {
  if (!axios.isAxiosError(error) || error.response?.status !== HTTP_UNAUTHORIZED) {
    return false;
  }
  const sentAuthorization = error.config?.headers?.Authorization;
  const currentToken = localStorage.getItem(TOKEN_KEY);
  return currentToken !== null && sentAuthorization === `${BEARER_PREFIX}${currentToken}`;
}

// 응답 인터셉터: 세션이 만료되면 토큰을 비우고 등록된 처리 함수를 부른다 (§7.2)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (isCurrentSessionExpired(error)) {
      localStorage.removeItem(TOKEN_KEY);
      sessionExpiredHandler?.();
    }
    return Promise.reject(error);
  },
);

export default api;
