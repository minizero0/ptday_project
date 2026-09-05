// 백엔드 MemberResponse 와 1:1 대응 (domain/member/dto/MemberResponse)
export interface Member {
  id: number;
  memberNo: string;
  name: string;
  phone: string | null;
  gender: string | null;
  birthDate: string | null; // yyyy-MM-dd
  createdAt: string; // ISO-8601 UTC — 표시할 때만 로컬 변환 (CLAUDE.md §5)
}
