import { useEffect } from 'react';
import { refreshToken } from '../api/authApi';
import { useAuthStore } from '../store/authStore';
import { ROLE_KIOSK } from '../types/auth';

// 토큰 유효기간(30일)보다 훨씬 자주 받아 둔다 — 몇 번 실패해도 다음 차례에 이어지게
const RENEWAL_INTERVAL_MS = 6 * 60 * 60 * 1000;

/**
 * 입구 키오스크는 로그인해 둔 채 계속 켜 두므로, 켜져 있는 동안 주기적으로 새 토큰을 받아 만료되지 않게 한다.
 * 키오스크 계정이 아니면 아무것도 하지 않는다(직원 세션은 정해진 시간 뒤에 만료돼야 한다).
 */
export function useKioskTokenRenewal(): void {
  const isKioskAccount = useAuthStore((state) => state.role) === ROLE_KIOSK;
  const setAuth = useAuthStore((state) => state.setAuth);

  useEffect(() => {
    if (!isKioskAccount) {
      return undefined;
    }

    const renew = () => {
      refreshToken()
        .then(setAuth)
        .catch(() => {
          // 일시적인 네트워크 오류는 다음 차례에 다시 시도하면 된다.
          // 계정이 지워져 401 이 온 경우는 공통 인터셉터가 로그인 화면으로 보낸다 (lib/api).
        });
    };

    renew();
    const timer = setInterval(renew, RENEWAL_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [isKioskAccount, setAuth]);
}
