import { z } from 'zod';
import { sessionMinutesSchema } from '../../pt-passes/types/ptPassSchema';
import { CLOSING_MINUTES, START_TIME_OPTIONS, parseMinutes, toInstant } from './timetable';

// FE 검증은 UX 보조. 시각 규칙의 최종 검증은 서버가 한다 (CLAUDE.md §6)
export const ptReservationCreateSchema = z
  .object({
    date: z.string().min(1, '날짜를 선택하세요.'),
    time: z.string().refine((value) => START_TIME_OPTIONS.includes(value), '시각을 선택하세요.'),
    sessionMinutes: sessionMinutesSchema,
  })
  .superRefine((values, context) => {
    const startMinutes = parseMinutes(values.time);
    if (startMinutes + values.sessionMinutes > CLOSING_MINUTES) {
      context.addIssue({
        code: 'custom',
        path: ['time'],
        message: '수업이 영업 종료(23:00) 전에 끝나야 합니다.',
      });
    }
    if (new Date(toInstant(values.date, startMinutes)).getTime() < Date.now()) {
      context.addIssue({
        code: 'custom',
        path: ['time'],
        message: '지난 시각에는 예약할 수 없습니다.',
      });
    }
  });

export type PtReservationCreateFormValues = z.infer<typeof ptReservationCreateSchema>;
