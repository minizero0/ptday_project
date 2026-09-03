import { create } from 'zustand';
import { TOKEN_KEY } from '../../../lib/api';
import type { LoginResponse } from '../types/auth';

interface AuthState {
  accessToken: string | null;
  username: string | null;
  role: string | null;
  isAuthenticated: boolean;
  setAuth: (response: LoginResponse) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  // 토큰은 localStorage 를 소스로 삼아 새로고침 후에도 로그인 유지
  accessToken: localStorage.getItem(TOKEN_KEY),
  username: null,
  role: null,
  isAuthenticated: Boolean(localStorage.getItem(TOKEN_KEY)),

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
    set({ accessToken: null, username: null, role: null, isAuthenticated: false });
  },
}));
