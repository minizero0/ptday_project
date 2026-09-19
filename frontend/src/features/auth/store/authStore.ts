import { create } from 'zustand';
import { TOKEN_KEY } from '../../../lib/api';
import { decodeJwtPayload, isJwtExpired } from '../../../lib/jwt';
import type { LoginResponse } from '../types/auth';

interface AuthState {
  accessToken: string | null;
  username: string | null;
  role: string | null;
  isAuthenticated: boolean;
  // 직접 로그아웃한 것이 아니라 세션이 만료돼 풀린 상태. 로그인 화면이 이유를 안내하는 데 쓴다.
  isSessionExpired: boolean;
  setAuth: (response: LoginResponse) => void;
  logout: () => void;
  expireSession: () => void;
}

type StoredSession = Pick<
  AuthState,
  'accessToken' | 'username' | 'role' | 'isAuthenticated' | 'isSessionExpired'
>;

const LOGGED_OUT_SESSION: StoredSession = {
  accessToken: null,
  username: null,
  role: null,
  isAuthenticated: false,
  isSessionExpired: false,
};

const EXPIRED_SESSION: StoredSession = { ...LOGGED_OUT_SESSION, isSessionExpired: true };

/**
 * 새로고침 후 로그인 상태 복원. 저장해 둔 것은 토큰 하나뿐이고 아이디·권한은 토큰에서 다시 읽는다 —
 * 따로 저장하면 토큰과 어긋날 수 있기 때문이다.
 * 읽을 수 없거나 이미 만료된 토큰은 어차피 첫 요청에서 401 이 나므로 처음부터 로그아웃 상태로 시작한다.
 */
function restoreSession(): StoredSession {
  const token = localStorage.getItem(TOKEN_KEY);
  if (!token) {
    return LOGGED_OUT_SESSION;
  }

  const payload = decodeJwtPayload(token);
  if (!payload) {
    localStorage.removeItem(TOKEN_KEY);
    return LOGGED_OUT_SESSION;
  }
  if (isJwtExpired(payload)) {
    // 어제 로그인해 둔 화면을 다시 연 경우 등. 왜 로그인 화면인지 알 수 있게 만료로 표시한다.
    localStorage.removeItem(TOKEN_KEY);
    return EXPIRED_SESSION;
  }

  return {
    accessToken: token,
    username: payload.sub,
    role: payload.role,
    isAuthenticated: true,
    isSessionExpired: false,
  };
}

export const useAuthStore = create<AuthState>((set) => ({
  ...restoreSession(),

  setAuth: (response) => {
    localStorage.setItem(TOKEN_KEY, response.accessToken);
    set({
      accessToken: response.accessToken,
      username: response.username,
      role: response.role,
      isAuthenticated: true,
      isSessionExpired: false,
    });
  },

  logout: () => {
    localStorage.removeItem(TOKEN_KEY);
    set(LOGGED_OUT_SESSION);
  },

  // 사용 중 서버가 토큰 만료(401)를 알려왔을 때. 요청 여러 개가 동시에 401 을 받아 거듭 불려도 결과가 같다.
  expireSession: () => {
    localStorage.removeItem(TOKEN_KEY);
    set(EXPIRED_SESSION);
  },
}));
