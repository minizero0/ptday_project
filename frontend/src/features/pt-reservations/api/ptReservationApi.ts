import api from '../../../lib/api';
import type { ApiResponse } from '../../../types/api';
import type {
  PtReservation,
  PtReservationCreateRequest,
  PtReservationQuery,
} from '../types/ptReservation';

// 트레이너 한 명의 기간 내 유효 예약 (취소된 예약은 내려오지 않는다)
export async function getPtReservations(query: PtReservationQuery): Promise<PtReservation[]> {
  const { data } = await api.get<ApiResponse<PtReservation[]>>('/api/pt-reservations', {
    params: query,
  });
  return data.data as PtReservation[];
}

export async function createPtReservation(
  request: PtReservationCreateRequest,
): Promise<PtReservation> {
  const { data } = await api.post<ApiResponse<PtReservation>>('/api/pt-reservations', request);
  return data.data as PtReservation;
}

// 취소하면 서버가 차감했던 PT권에 1회를 돌려준다
export async function cancelPtReservation(reservationId: number): Promise<PtReservation> {
  const { data } = await api.post<ApiResponse<PtReservation>>(
    `/api/pt-reservations/${reservationId}/cancel`,
  );
  return data.data as PtReservation;
}
