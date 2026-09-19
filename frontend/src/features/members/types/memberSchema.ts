import { z } from 'zod';
import { todayString } from '../../../lib/date';
import { PHONE_MAX_DIGITS, extractDigits, formatPhone, isValidPhone } from '../../../lib/phone';
import type { Member, MemberSaveRequest } from './member';

// 서버 DTO(MemberCreateRequest / MemberUpdateRequest) 제약과 같은 값
const NAME_MAX_LENGTH = 50;
const PHONE_MAX_LENGTH = 20;

// 기존 데이터가 쓰는 저장 값. 빈 문자열은 "선택 안 함"
export const GENDER_OPTIONS = ['남', '여'] as const;

// FE 검증은 UX 보조. 최종 검증 책임은 백엔드 (CLAUDE.md §6)
// 입력창 값은 전부 문자열이라 스키마도 문자열로 받고, 서버로 보낼 때 빈 값을 null 로 바꾼다.
export const memberSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, '이름을 입력하세요.')
    .max(NAME_MAX_LENGTH, `이름은 ${NAME_MAX_LENGTH}자 이하여야 합니다.`),
  phone: z
    .string()
    .trim()
    .max(PHONE_MAX_LENGTH, `전화번호는 ${PHONE_MAX_LENGTH}자 이하여야 합니다.`)
    .refine(isValidPhone, '전화번호 형식을 확인하세요. (예: 010-1234-5678)'),
  gender: z.enum(['', ...GENDER_OPTIONS]),
  // yyyy-MM-dd 는 문자열 비교가 곧 날짜 비교다. 서버 @Past 와 같이 오늘은 허용하지 않는다.
  birthDate: z
    .string()
    .refine((value) => value === '' || value < todayString(), '생년월일은 과거 날짜여야 합니다.'),
});

export type MemberFormValues = z.infer<typeof memberSchema>;

export const EMPTY_MEMBER_FORM: MemberFormValues = {
  name: '',
  phone: '',
  gender: '',
  birthDate: '',
};

function isGenderOption(value: string | null): value is (typeof GENDER_OPTIONS)[number] {
  return GENDER_OPTIONS.some((option) => option === value);
}

// 저장된 번호를 입력부와 같은 표기로 맞춘다. 자릿수가 넘치는 옛 데이터는 포맷 과정에서
// 뒷자리가 조용히 잘리지 않도록 원본 그대로 보여주고, 검증 메시지로 고치게 한다.
function toPhoneFormValue(phone: string | null): string {
  if (phone === null) {
    return '';
  }
  return extractDigits(phone).length > PHONE_MAX_DIGITS ? phone : formatPhone(phone);
}

// 수정 모달의 초기값. 선택지에 없는 성별 값은 "선택 안 함"으로 보여준다.
export function toMemberFormValues(member: Member): MemberFormValues {
  return {
    name: member.name,
    phone: toPhoneFormValue(member.phone),
    gender: isGenderOption(member.gender) ? member.gender : '',
    birthDate: member.birthDate ?? '',
  };
}

// 폼 값 → 서버 요청. 비워 둔 선택 항목은 null 로 보낸다.
export function toMemberSaveRequest(values: MemberFormValues): MemberSaveRequest {
  return {
    name: values.name,
    phone: values.phone || null,
    gender: values.gender || null,
    birthDate: values.birthDate || null,
  };
}
