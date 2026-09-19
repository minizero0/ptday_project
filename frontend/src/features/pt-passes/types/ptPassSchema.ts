import { z } from 'zod';
import { MAX_PT_COUNT } from './ptPass';
import type { PtPassAdjustRequest } from './ptPass';

// FE 검증은 UX 보조. 최종 검증 책임은 백엔드 (CLAUDE.md §6)
const countSchema = z
  .number({ error: '횟수를 숫자로 입력하세요.' })
  .int('횟수는 정수로 입력하세요.')
  .min(1, '횟수는 1회 이상이어야 합니다.')
  .max(MAX_PT_COUNT, `횟수는 ${MAX_PT_COUNT}회 이하여야 합니다.`);

export const ptPassCreateSchema = z.object({ totalCount: countSchema });

export type PtPassCreateFormValues = z.infer<typeof ptPassCreateSchema>;

const REASON_MAX_LENGTH = 200;

export const ADJUST_DIRECTIONS = ['ADD', 'SUBTRACT'] as const;
export type AdjustDirection = (typeof ADJUST_DIRECTIONS)[number];

/**
 * 조정 폼. 부호를 직접 치게 하면 실수하기 쉬워 "추가/차감"과 횟수를 따로 받는다.
 * 뺄 수 있는 한도는 지금 잔여 횟수에 달려 있어서 스키마를 잔여 횟수로 만든다.
 */
export function createPtPassAdjustSchema(remainingCount: number) {
  return z
    .object({
      direction: z.enum(ADJUST_DIRECTIONS),
      count: countSchema,
      reason: z
        .string()
        .trim()
        .min(1, '사유를 입력하세요.')
        .max(REASON_MAX_LENGTH, `사유는 ${REASON_MAX_LENGTH}자 이하여야 합니다.`),
    })
    .superRefine((values, context) => {
      if (values.direction === 'SUBTRACT' && values.count > remainingCount) {
        context.addIssue({
          code: 'custom',
          path: ['count'],
          message: `잔여 횟수(${remainingCount}회)보다 많이 뺄 수 없습니다.`,
        });
      }
      if (values.direction === 'ADD' && remainingCount + values.count > MAX_PT_COUNT) {
        context.addIssue({
          code: 'custom',
          path: ['count'],
          message: `잔여 횟수는 ${MAX_PT_COUNT}회를 넘을 수 없습니다.`,
        });
      }
    });
}

export type PtPassAdjustFormValues = z.infer<ReturnType<typeof createPtPassAdjustSchema>>;

export function toSignedDelta(direction: AdjustDirection, count: number): number {
  return direction === 'ADD' ? count : -count;
}

export function toPtPassAdjustRequest(values: PtPassAdjustFormValues): PtPassAdjustRequest {
  return { delta: toSignedDelta(values.direction, values.count), reason: values.reason };
}
