import { z } from 'zod';
import { formatPhone, isValidPhone } from '../../../lib/phone';
import type { Trainer, TrainerSaveRequest } from './trainer';

const NAME_MAX_LENGTH = 50;
const PHONE_MAX_LENGTH = 20;

// FE 검증은 UX 보조. 최종 검증 책임은 백엔드 (CLAUDE.md §6)
export const trainerSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, '이름을 입력하세요.')
    .max(NAME_MAX_LENGTH, `이름은 ${NAME_MAX_LENGTH}자 이하여야 합니다.`),
  phone: z
    .string()
    .max(PHONE_MAX_LENGTH, `전화번호는 ${PHONE_MAX_LENGTH}자 이하여야 합니다.`)
    .refine(isValidPhone, '전화번호 형식을 확인하세요. (예: 010-1234-5678)'),
});

export type TrainerFormValues = z.infer<typeof trainerSchema>;

export const EMPTY_TRAINER_FORM: TrainerFormValues = { name: '', phone: '' };

export function toTrainerFormValues(trainer: Trainer): TrainerFormValues {
  return { name: trainer.name, phone: trainer.phone ? formatPhone(trainer.phone) : '' };
}

export function toTrainerSaveRequest(values: TrainerFormValues): TrainerSaveRequest {
  return { name: values.name, phone: values.phone === '' ? null : values.phone };
}
