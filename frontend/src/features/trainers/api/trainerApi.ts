import api from '../../../lib/api';
import type { ApiResponse } from '../../../types/api';
import type { Trainer, TrainerSaveRequest } from '../types/trainer';

// 기본은 재직 중인 트레이너만. 관리 화면은 includeInactive 로 비활성까지 받는다.
export async function getTrainers(includeInactive: boolean): Promise<Trainer[]> {
  const { data } = await api.get<ApiResponse<Trainer[]>>('/api/trainers', {
    params: { includeInactive },
  });
  return data.data as Trainer[];
}

export async function createTrainer(request: TrainerSaveRequest): Promise<Trainer> {
  const { data } = await api.post<ApiResponse<Trainer>>('/api/trainers', request);
  return data.data as Trainer;
}

export async function updateTrainer(id: number, request: TrainerSaveRequest): Promise<Trainer> {
  const { data } = await api.patch<ApiResponse<Trainer>>(`/api/trainers/${id}`, request);
  return data.data as Trainer;
}
