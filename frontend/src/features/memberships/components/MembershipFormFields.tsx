import { useState } from 'react';
import { Input } from '../../../components/Input';
import { addMonths } from '../../../lib/date';
import { MEMBERSHIP_PLAN_MONTHS } from '../types/membership';
import type { MembershipPeriodRequest } from '../types/membership';

interface MembershipFormFieldsProps {
  value: MembershipPeriodRequest;
  onChange: (next: MembershipPeriodRequest) => void;
}

// 직접 입력으로 채울 수 있는 개월 수 범위 (만료일 계산용 상한)
const MAX_CUSTOM_MONTHS = 36;

/**
 * 이용권 기간 입력부. 저장되는 값은 시작일·만료일 두 개뿐이고,
 * 개월 수는 만료일을 채워 주는 계산 수단이라 이 컴포넌트 안에만 머문다.
 * 모달/전용 화면 어디서든 쓰도록 껍데기와 분리했다.
 */
export function MembershipFormFields({ value, onChange }: MembershipFormFieldsProps) {
  const [customMonths, setCustomMonths] = useState('');

  const applyMonths = (months: number) => {
    if (!value.startDate || !Number.isFinite(months) || months < 1) {
      return;
    }
    onChange({ ...value, endDate: addMonths(value.startDate, months) });
  };

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Input
          label="시작일"
          type="date"
          value={value.startDate}
          onChange={(event) => onChange({ ...value, startDate: event.target.value })}
        />
        <Input
          label="만료일"
          type="date"
          value={value.endDate}
          onChange={(event) => onChange({ ...value, endDate: event.target.value })}
        />
      </div>

      <div>
        <span className="mb-1 block text-sm font-medium">만료일 빠른 계산</span>
        <div className="flex flex-wrap items-center gap-2">
          {MEMBERSHIP_PLAN_MONTHS.map((months) => (
            <button
              key={months}
              type="button"
              disabled={!value.startDate}
              onClick={() => applyMonths(months)}
              className="rounded-md border border-border bg-surface px-3 py-1.5 text-sm hover:bg-background disabled:opacity-50"
            >
              +{months}개월
            </button>
          ))}
          <span className="flex items-center gap-1">
            <input
              type="number"
              min={1}
              max={MAX_CUSTOM_MONTHS}
              aria-label="개월 수 직접 입력"
              placeholder="직접"
              value={customMonths}
              onChange={(event) => setCustomMonths(event.target.value)}
              className="w-20 rounded-md border border-border bg-surface px-2 py-1.5 text-sm placeholder:text-text-muted"
            />
            <button
              type="button"
              disabled={!value.startDate || customMonths === ''}
              onClick={() => applyMonths(Number(customMonths))}
              className="rounded-md border border-border bg-surface px-3 py-1.5 text-sm hover:bg-background disabled:opacity-50"
            >
              적용
            </button>
          </span>
        </div>
        <p className="mt-1 text-xs text-text-muted">
          버튼을 누르면 시작일 기준으로 만료일이 채워집니다. 만료일은 언제든 직접 고칠 수 있습니다.
        </p>
      </div>
    </div>
  );
}
