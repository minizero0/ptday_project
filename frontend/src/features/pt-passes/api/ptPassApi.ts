import api from '../../../lib/api';
import type { ApiResponse } from '../../../types/api';
import type {
  PtPass,
  PtPassAdjustment,
  PtPassAdjustRequest,
  PtPassCreateRequest,
} from '../types/ptPass';

// 회원의 PT권 목록 — 최근 구매분이 위로 내려온다
export async function getPtPasses(memberId: number): Promise<PtPass[]> {
  const { data } = await api.get<ApiResponse<PtPass[]>>(`/api/members/${memberId}/pt-passes`);
  return data.data as PtPass[];
}

// PT권 부여
export async function grantPtPass(memberId: number, request: PtPassCreateRequest): Promise<PtPass> {
  const { data } = await api.post<ApiResponse<PtPass>>(
    `/api/members/${memberId}/pt-passes`,
    request,
  );
  return data.data as PtPass;
}

// 잔여 횟수 수동 조정. 조정이 반영된 PT권이 돌아온다.
export async function adjustPtPass(
  ptPassId: number,
  request: PtPassAdjustRequest,
): Promise<PtPass> {
  const { data } = await api.post<ApiResponse<PtPass>>(
    `/api/pt-passes/${ptPassId}/adjustments`,
    request,
  );
  return data.data as PtPass;
}

// 횟수 변동 이력 — 최근 변동이 위로 내려온다
export async function getPtPassAdjustments(ptPassId: number): Promise<PtPassAdjustment[]> {
  const { data } = await api.get<ApiResponse<PtPassAdjustment[]>>(
    `/api/pt-passes/${ptPassId}/adjustments`,
  );
  return data.data as PtPassAdjustment[];
}
