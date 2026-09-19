import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createTrainer, getTrainers, updateTrainer } from '../api/trainerApi';
import type { TrainerSaveRequest } from '../types/trainer';

// 쿼리 키 컨벤션: ['trainers', { includeInactive }] (CLAUDE.md §6)
const trainerKeys = {
  list: (includeInactive: boolean) => ['trainers', { includeInactive }] as const,
};

// 예약 응답에 트레이너 이름이 담겨 있어, 이름을 고치면 시간표도 낡는다.
const TRAINER_DEPENDENT_QUERY_ROOTS = ['trainers', 'pt-reservations'] as const;

export function useTrainersQuery(includeInactive = false) {
  return useQuery({
    queryKey: trainerKeys.list(includeInactive),
    queryFn: () => getTrainers(includeInactive),
  });
}

export function useCreateTrainerMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: TrainerSaveRequest) => createTrainer(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['trainers'] }),
  });
}

interface UpdateTrainerVariables {
  id: number;
  request: TrainerSaveRequest;
}

export function useUpdateTrainerMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, request }: UpdateTrainerVariables) => updateTrainer(id, request),
    onSuccess: () => {
      TRAINER_DEPENDENT_QUERY_ROOTS.forEach((root) => {
        queryClient.invalidateQueries({ queryKey: [root] });
      });
    },
  });
}
