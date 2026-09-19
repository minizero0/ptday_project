import { create } from 'zustand';
import { TOKEN_KEY } from '../../../lib/api';
import { decodeJwtPayload, isJwtExpired } from '../../../lib/jwt';
import type { LoginResponse } from '../types/auth';

interface AuthState {
  accessToken: string | null;
  username: string | null;
  role: string | null;
  isAuthenticated: boolean;
  setAuth: (response: LoginResponse) => void;
  logout: () => void;
}

type StoredSession = Pick<AuthState, 'accessToken' | 'username' | 'role' | 'isAuthenticated'>;

const LOGGED_OUT_SESSION: StoredSession = {
  accessToken: null,
  username: null,
  role: null,
  isAuthenticated: false,
};

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
  if (!payload || isJwtExpired(payload)) {
    localStorage.removeItem(TOKEN_KEY);
    return LOGGED_OUT_SESSION;
  }

  return {
    accessToken: token,
    username: payload.sub,
    role: payload.role,
    isAuthenticated: true,
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
    });
  },

  logout: () => {
    localStorage.removeItem(TOKEN_KEY);
    set(LOGGED_OUT_SESSION);
  },
}));
