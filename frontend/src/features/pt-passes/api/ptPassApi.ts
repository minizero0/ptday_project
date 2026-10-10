import api from '../../../lib/api';
import type { ApiResponse, PageResponse } from '../../../types/api';
import type {
  PtPass,
  PtPassAdjustment,
  PtPassAdjustRequest,
  PtPassCreateRequest,
  PtPassListItem,
  PtPassSearchFilter,
} from '../types/ptPass';

// 회원의 PT권 목록 — 최근 구매분이 위로 내려온다
export async function getPtPasses(memberId: number): Promise<PtPass[]> {
  const { data } = await api.get<ApiResponse<PtPass[]>>(`/api/members/${memberId}/pt-passes`);
  return data.data as PtPass[];
}

// PT권 관리 목록 — 최근 구매순. 비어 있는 검색 조건은 보내지 않는다.
export async function getPtPassPage(
  filter: PtPassSearchFilter,
  page: number,
  size: number,
): Promise<PageResponse<PtPassListItem>> {
  const { data } = await api.get<ApiResponse<PageResponse<PtPassListItem>>>('/api/pt-passes', {
    params: {
      includeExhausted: filter.includeExhausted,
      name: filter.name || undefined,
      purchasedFrom: filter.purchasedFrom || undefined,
      purchasedTo: filter.purchasedTo || undefined,
      page,
      size,
    },
  });
  return data.data as PageResponse<PtPassListItem>;
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
