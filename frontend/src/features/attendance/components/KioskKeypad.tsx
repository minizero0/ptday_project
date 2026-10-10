interface KioskKeypadProps {
  disabled: boolean;
  onDigit: (digit: string) => void;
  onBackspace: () => void;
  onClear: () => void;
}

const DIGIT_ROWS = [
  ['1', '2', '3'],
  ['4', '5', '6'],
  ['7', '8', '9'],
] as const;

// 입구에서 손가락으로 누르는 버튼이라 관리 화면의 공용 Button 보다 훨씬 크게 잡는다
const KEY_CLASS_NAME =
  'flex h-20 items-center justify-center rounded-xl border border-border bg-surface text-3xl font-semibold shadow-card transition-colors active:bg-primary/20 disabled:cursor-not-allowed disabled:opacity-50 sm:h-24';
const ACTION_KEY_CLASS_NAME = `${KEY_CLASS_NAME} text-lg font-medium text-text-muted`;

/** 출석 키오스크의 숫자 키패드. 무엇을 입력 중인지는 모르고 누른 키만 알린다. */
export function KioskKeypad({ disabled, onDigit, onBackspace, onClear }: KioskKeypadProps) {
  return (
    <div className="grid grid-cols-3 gap-3">
      {DIGIT_ROWS.flat().map((digit) => (
        <button
          key={digit}
          type="button"
          disabled={disabled}
          onClick={() => onDigit(digit)}
          className={KEY_CLASS_NAME}
        >
          {digit}
        </button>
      ))}
      <button type="button" disabled={disabled} onClick={onClear} className={ACTION_KEY_CLASS_NAME}>
        전체 지움
      </button>
      <button type="button" disabled={disabled} onClick={() => onDigit('0')} className={KEY_CLASS_NAME}>
        0
      </button>
      <button
        type="button"
        disabled={disabled}
        onClick={onBackspace}
        className={ACTION_KEY_CLASS_NAME}
      >
        지움
      </button>
    </div>
  );
}
