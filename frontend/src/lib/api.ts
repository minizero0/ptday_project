import axios from 'axios';

export const TOKEN_KEY = 'accessToken';

// 기본은 상대경로('') — /api 요청이 Vite 프록시(개발)·nginx(운영)를 거쳐 백엔드로 간다.
// 필요 시 .env 의 VITE_API_BASE_URL 로 절대주소를 덮어쓸 수 있다.
const baseURL = import.meta.env.VITE_API_BASE_URL ?? '';

const api = axios.create({ baseURL });

// 요청 인터셉터: 저장된 토큰이 있으면 Authorization 헤더에 자동 첨부
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 응답 인터셉터: 401(인증 만료/실패)이면 토큰을 비운다 (§7.2)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      // 라우터 도입 후 로그인 페이지로 리다이렉트 예정
    }
    return Promise.reject(error);
  },
);

export default api;
