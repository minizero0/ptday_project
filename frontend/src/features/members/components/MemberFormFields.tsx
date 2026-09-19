import { useId } from 'react';
import type { FieldErrors, UseFormRegister } from 'react-hook-form';
import { Input } from '../../../components/Input';
import { todayString } from '../../../lib/date';
import { formatPhone } from '../../../lib/phone';
import { GENDER_OPTIONS } from '../types/memberSchema';
import type { MemberFormValues } from '../types/memberSchema';

interface MemberFormFieldsProps {
  register: UseFormRegister<MemberFormValues>;
  errors: FieldErrors<MemberFormValues>;
}

/**
 * 회원 정보 입력부. 등록·수정 모달이 같은 필드를 쓰므로 껍데기(모달)와 분리했다.
 * 회원번호는 서버가 채번하므로 입력받지 않는다.
 */
export function MemberFormFields({ register, errors }: MemberFormFieldsProps) {
  const genderId = useId();
  const phoneField = register('phone');

  return (
    <div className="space-y-4">
      <Input
        label="이름"
        placeholder="홍길동"
        autoComplete="off"
        error={errors.name?.message}
        {...register('name')}
      />
      <Input
        label="전화번호"
        type="tel"
        inputMode="numeric"
        placeholder="010-0000-0000"
        autoComplete="off"
        error={errors.phone?.message}
        {...phoneField}
        onChange={(event) => {
          // 폼 라이브러리가 입력창 값을 직접 읽으므로, 값을 넘기기 전에 입력창 표기부터 하이픈 형식으로 바꾼다
          event.target.value = formatPhone(event.target.value);
          return phoneField.onChange(event);
        }}
      />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="w-full">
          <label htmlFor={genderId} className="mb-1 block text-sm font-medium">
            성별
          </label>
          <select
            id={genderId}
            className="w-full rounded-md border border-border bg-surface px-3 py-2 text-sm"
            {...register('gender')}
          >
            <option value="">선택 안 함</option>
            {GENDER_OPTIONS.map((gender) => (
              <option key={gender} value={gender}>
                {gender}
              </option>
            ))}
          </select>
        </div>

        <Input
          label="생년월일"
          type="date"
          max={todayString()}
          error={errors.birthDate?.message}
          {...register('birthDate')}
        />
      </div>
    </div>
  );
}
