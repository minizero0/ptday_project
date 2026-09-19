// 백엔드 TrainerResponse 와 1:1 대응 (domain/trainer/dto/TrainerResponse)
export interface Trainer {
  id: number;
  name: string;
  phone: string | null;
  // 퇴사 등으로 비활성 처리된 트레이너. 지난 예약이 참조하므로 지우지 않는다 — 새 예약만 막힌다.
  active: boolean;
}

// 등록·수정 공용 요청. active 는 수정에서만 의미가 있고, 빼면 서버가 그대로 둔다.
export interface TrainerSaveRequest {
  name: string;
  phone: string | null;
  active?: boolean;
}
