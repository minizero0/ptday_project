import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../../../components/Button';
import { Modal } from '../../../components/Modal';
import { getApiErrorMessage } from '../../../lib/apiError';
import { formatDay, todayString } from '../../../lib/date';
import { useKioskTokenRenewal } from '../../auth/hooks/useKioskTokenRenewal';
import { useAuthStore } from '../../auth/store/authStore';
import { ROLE_KIOSK } from '../../auth/types/auth';
import { KioskKeypad } from '../components/KioskKeypad';
import { useCheckInMutation } from '../hooks/useAttendance';
import { useKioskSound } from '../hooks/useKioskSound';
import type { Attendance, CheckInCandidate, CheckInRequest } from '../types/attendance';

// 서버(AttendanceService.PHONE_LAST_DIGITS_LENGTH)와 같은 값
const PHONE_LAST_DIGITS_LENGTH = 4;
// 결과를 읽을 만큼만 보여주고 다음 사람을 위해 입력 화면으로 돌아간다
const RESULT_DISPLAY_MS = 4_000;
// 만료 임박 안내는 읽을 글이 더 있으니 조금 더 보여준다
const NOTICE_DISPLAY_MS = 7_000;
// 이용권 만료가 이 일수 이내로 남으면 출석할 때 알려준다
const EXPIRY_NOTICE_DAYS = 3;
// 후보를 고르다 자리를 뜬 경우 남의 화면이 계속 떠 있지 않게 한다
const CANDIDATE_TIMEOUT_MS = 20_000;
const CHECK_IN_ERROR_FALLBACK = '출석 처리에 실패했습니다. 데스크에 문의해주세요.';

type KioskView =
  | { kind: 'input' }
  | { kind: 'success'; attendance: Attendance; expiryNotice: string | null }
  | { kind: 'candidates'; candidates: CheckInCandidate[] }
  | { kind: 'error'; message: string };

const INPUT_VIEW: KioskView = { kind: 'input' };

// 만료가 가까운 회원에게 보여줄 안내. 아직 여유가 있으면 null.
function toExpiryNotice(attendance: Attendance): string | null {
  const daysRemaining = attendance.membership?.daysRemaining;
  if (daysRemaining == null || daysRemaining > EXPIRY_NOTICE_DAYS) {
    return null;
  }
  if (daysRemaining === 0) {
    return '이용권이 오늘 만료됩니다.';
  }
  if (daysRemaining === 1) {
    return '이용권 만료 하루 전입니다.';
  }
  return `이용권이 ${daysRemaining}일 남았습니다.`;
}

function getDisplayMs(view: KioskView): number {
  if (view.kind === 'candidates') {
    return CANDIDATE_TIMEOUT_MS;
  }
  return view.kind === 'success' && view.expiryNotice ? NOTICE_DISPLAY_MS : RESULT_DISPLAY_MS;
}

/** 회원이 입구에서 휴대폰 번호 뒷 4자리를 직접 눌러 출석하는 화면. 관리 메뉴 없이 전체 화면으로 뜬다. */
export function KioskPage() {
  const navigate = useNavigate();
  const role = useAuthStore((state) => state.role);
  const logout = useAuthStore((state) => state.logout);
  const [digits, setDigits] = useState('');
  const [view, setView] = useState<KioskView>(INPUT_VIEW);
  const [isExitConfirmOpen, setIsExitConfirmOpen] = useState(false);
  const checkInMutation = useCheckInMutation(todayString(), true);
  const { playSuccess, playNotice, playError } = useKioskSound();
  useKioskTokenRenewal();

  const isKioskAccount = role === ROLE_KIOSK;
  const isSubmitting = checkInMutation.isPending;

  const handleReset = () => {
    setDigits('');
    setView(INPUT_VIEW);
  };

  const submit = (request: CheckInRequest) => {
    checkInMutation.mutate(request, {
      onSuccess: (result) => {
        setDigits('');
        if (result.status === 'CHECKED_IN' && result.attendance) {
          const expiryNotice = toExpiryNotice(result.attendance);
          setView({ kind: 'success', attendance: result.attendance, expiryNotice });
          if (expiryNotice) {
            playNotice();
          } else {
            playSuccess();
          }
          return;
        }
        setView({ kind: 'candidates', candidates: result.candidates ?? [] });
      },
      onError: (error) => {
        setDigits('');
        setView({ kind: 'error', message: getApiErrorMessage(error, CHECK_IN_ERROR_FALLBACK) });
        playError();
      },
    });
  };

  const handleDigit = (digit: string) => {
    if (isSubmitting || view.kind === 'candidates') {
      return;
    }
    // 결과 화면이 떠 있는 동안 다음 사람이 바로 누르기 시작해도 받아 준다
    const next = (view.kind === 'input' ? digits : '') + digit;
    setView(INPUT_VIEW);
    setDigits(next);
    if (next.length === PHONE_LAST_DIGITS_LENGTH) {
      submit({ phoneLastDigits: next });
    }
  };

  const handleBackspace = () => {
    if (!isSubmitting && view.kind === 'input') {
      setDigits(digits.slice(0, -1));
    }
  };

  const handleExit = () => {
    if (isKioskAccount) {
      setIsExitConfirmOpen(true);
      return;
    }
    navigate('/');
  };

  // 결과·후보 화면은 일정 시간이 지나면 스스로 입력 화면으로 돌아간다
  useEffect(() => {
    if (view.kind === 'input') {
      return undefined;
    }
    const timer = setTimeout(() => {
      setDigits('');
      setView(INPUT_VIEW);
    }, getDisplayMs(view));
    return () => clearTimeout(timer);
  }, [view]);

  // 태블릿 대신 숫자 키패드 기기를 연결해 쓰는 경우를 위한 키보드 입력.
  // 핸들러가 매 렌더의 최신 상태를 보도록 의존성 배열 없이 매번 다시 건다.
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (isExitConfirmOpen) {
        return;
      }
      if (/^\d$/.test(event.key)) {
        handleDigit(event.key);
      } else if (event.key === 'Backspace') {
        handleBackspace();
      } else if (event.key === 'Escape') {
        handleReset();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  });

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="flex items-center justify-between px-4 py-3">
        <p className="text-lg font-bold">ptday 출석</p>
        <Button variant="ghost" size="sm" onClick={handleExit}>
          {isKioskAccount ? '종료' : '관리 화면'}
        </Button>
      </header>

      <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center gap-6 px-4 pb-8">
        {view.kind === 'input' && (
          <>
            <div className="text-center">
              <h1 className="text-2xl font-bold">휴대폰 번호 뒷 4자리를 눌러주세요</h1>
              <p className="mt-2 text-sm text-text-muted" aria-live="polite">
                {isSubmitting ? '확인 중입니다...' : '4자리를 모두 누르면 바로 출석됩니다.'}
              </p>
            </div>
            <div
              className="flex justify-center gap-3"
              role="status"
              aria-label={`입력한 번호 ${digits.length}자리`}
            >
              {Array.from({ length: PHONE_LAST_DIGITS_LENGTH }, (_, index) => (
                <span
                  key={index}
                  className={`flex h-16 w-14 items-center justify-center rounded-xl border-2 bg-surface text-3xl font-bold ${
                    index < digits.length ? 'border-primary' : 'border-border'
                  }`}
                >
                  {digits[index] ?? ''}
                </span>
              ))}
            </div>
          </>
        )}

        {view.kind === 'success' && (
          <div role="status" className="rounded-xl border-2 border-success bg-surface p-8 text-center">
            <p className="text-3xl font-bold">{view.attendance.name}님</p>
            <p className="mt-2 text-xl font-semibold text-success">출석되었습니다</p>
            {view.expiryNotice && (
              <p className="mt-5 rounded-xl bg-warning/10 px-4 py-3 text-xl font-bold text-warning">
                {view.expiryNotice}
              </p>
            )}
            {view.attendance.membership && (
              <p className="mt-4 text-sm text-text-muted">
                이용권 만료일 {formatDay(view.attendance.membership.endDate)}
              </p>
            )}
          </div>
        )}

        {view.kind === 'error' && (
          <div role="alert" className="rounded-xl border-2 border-danger bg-surface p-8 text-center">
            <p className="text-xl font-semibold text-danger">{view.message}</p>
            <p className="mt-3 text-sm text-text-muted">번호를 다시 눌러주세요.</p>
          </div>
        )}

        {view.kind === 'candidates' ? (
          <div className="rounded-xl border border-border bg-surface p-6">
            <h1 className="text-center text-xl font-bold">본인을 선택해주세요</h1>
            <p className="mt-1 text-center text-sm text-text-muted">
              같은 번호를 쓰는 회원이 여러 명입니다.
            </p>
            <ul className="mt-4 space-y-3">
              {view.candidates.map((candidate) => (
                <li key={candidate.memberId}>
                  <button
                    type="button"
                    disabled={isSubmitting}
                    onClick={() => submit({ memberId: candidate.memberId })}
                    className="flex w-full items-center justify-between rounded-xl border border-border bg-surface px-5 py-4 text-left transition-colors active:bg-primary/20 disabled:opacity-50"
                  >
                    <span className="text-2xl font-semibold">{candidate.name}</span>
                    <span className="text-sm text-text-muted">{candidate.maskedPhone ?? ''}</span>
                  </button>
                </li>
              ))}
            </ul>
            <Button variant="ghost" className="mt-4 w-full py-3" onClick={handleReset}>
              취소
            </Button>
          </div>
        ) : (
          <KioskKeypad
            disabled={isSubmitting}
            onDigit={handleDigit}
            onBackspace={handleBackspace}
            onClear={handleReset}
          />
        )}
      </main>

      {isExitConfirmOpen && (
        <Modal title="키오스크 종료" onClose={() => setIsExitConfirmOpen(false)}>
          <p className="text-sm">
            로그아웃하면 직원이 다시 로그인할 때까지 출석 체크를 할 수 없습니다. 종료할까요?
          </p>
          <div className="mt-4 flex justify-end gap-2 border-t border-border pt-4">
            <Button variant="ghost" onClick={() => setIsExitConfirmOpen(false)}>
              취소
            </Button>
            <Button variant="danger" onClick={logout}>
              로그아웃
            </Button>
          </div>
        </Modal>
      )}
    </div>
  );
}
