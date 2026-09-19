// 국내 전화번호 자릿수. 서울 유선(02-123-4567)이 9자리로 가장 짧고 휴대폰이 11자리로 가장 길다.
export const PHONE_MIN_DIGITS = 9;
export const PHONE_MAX_DIGITS = 11;

const SEOUL_AREA_CODE = '02';
const MOBILE_PREFIX = '010';

export function extractDigits(value: string): string {
  return value.replace(/\D/g, '');
}

// 앞에서부터 주어진 길이대로 잘라 하이픈으로 잇는다. 아직 입력되지 않은 뒤쪽 조각은 버린다.
function joinGroups(digits: string, groupLengths: number[]): string {
  const groups: string[] = [];
  let offset = 0;
  for (const length of groupLengths) {
    const group = digits.slice(offset, offset + length);
    if (group === '') {
      break;
    }
    groups.push(group);
    offset += length;
  }
  return groups.join('-');
}

/**
 * 입력 중인 전화번호에 하이픈을 넣는다. 하이픈 유무와 상관없이 받아 표기를 하나로 맞추기 위함.
 * - 02 로 시작: 02-123-4567 / 02-1234-5678
 * - 010 으로 시작: 처음부터 010-1234-5678 (입력 도중 하이픈 위치가 바뀌지 않게)
 * - 그 외(지역번호, 011 등): 031-123-4567 / 031-1234-5678
 */
export function formatPhone(value: string): string {
  const digits = extractDigits(value).slice(0, PHONE_MAX_DIGITS);

  if (digits.startsWith(SEOUL_AREA_CODE)) {
    const seoulDigits = digits.slice(0, PHONE_MAX_DIGITS - 1);
    const middleLength = seoulDigits.length > 9 ? 4 : 3;
    return joinGroups(seoulDigits, [2, middleLength, 4]);
  }

  const middleLength = digits.startsWith(MOBILE_PREFIX) || digits.length > 10 ? 4 : 3;
  return joinGroups(digits, [3, middleLength, 4]);
}

// 비워 두거나(선택 항목), 0 으로 시작하는 9~11자리 숫자여야 한다. 하이픈은 입력부가 자동으로 넣는다.
export function isValidPhone(value: string): boolean {
  if (value === '') {
    return true;
  }
  const digits = extractDigits(value);
  return (
    digits.startsWith('0') &&
    digits.length >= PHONE_MIN_DIGITS &&
    digits.length <= PHONE_MAX_DIGITS
  );
}
