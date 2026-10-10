import { z } from 'zod';
import { sessionMinutesSchema } from '../../pt-passes/types/ptPassSchema';
import {
  CLOSING_MINUTES,
  START_MINUTE_OPTIONS,
  TIMETABLE_HOURS,
  toInstant,
  toMinutes,
} from './timetable';

// FE 검증은 UX 보조. 시각 규칙의 최종 검증은 서버가 한다 (CLAUDE.md §6)
export const ptReservationCreateSchema = z
  .object({
    date: z.string().min(1, '날짜를 선택하세요.'),
    // 시작 시각은 시·분을 따로 고른다. 오류는 한 줄로 보여 주려고 모두 hour 에 단다.
    hour: z.number().refine((value) => TIMETABLE_HOURS.includes(value), '시작 시간을 선택하세요.'),
    minute: z
      .number()
      .refine((value) => START_MINUTE_OPTIONS.includes(value), '시작 분을 선택하세요.'),
    sessionMinutes: sessionMinutesSchema,
  })
  .superRefine((values, context) => {
    const startMinutes = toMinutes(values.hour, values.minute);
    if (startMinutes + values.sessionMinutes > CLOSING_MINUTES) {
      context.addIssue({
        code: 'custom',
        path: ['hour'],
        message: '수업이 영업 종료(23:00) 전에 끝나야 합니다.',
      });
    }
    if (new Date(toInstant(values.date, startMinutes)).getTime() < Date.now()) {
      context.addIssue({
        code: 'custom',
        path: ['hour'],
        message: '지난 시각에는 예약할 수 없습니다.',
      });
    }
  });

export type PtReservationCreateFormValues = z.infer<typeof ptReservationCreateSchema>;
