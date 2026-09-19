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

// 백엔드 MemberCreateRequest / MemberUpdateRequest 와 1:1 대응 (두 요청의 필드가 같다).
// 회원번호는 서버가 자동 채번하므로 보내지 않는다.
export interface MemberSaveRequest {
  name: string;
  phone: string | null;
  gender: string | null;
  birthDate: string | null; // yyyy-MM-dd
}
