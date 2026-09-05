import { useMemberQuery } from '../hooks/useMember';

interface MemberDetailPanelProps {
  memberId: number;
  onClose: () => void;
}

// ISO-8601 UTC → 로컬 yyyy-MM-dd (CLAUDE.md §5)
function formatDate(instant: string): string {
  return new Date(instant).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}

function DetailRow({ label, value }: { label: string; value: string | null }) {
  return (
    <div className="flex justify-between gap-4 py-2">
      <dt className="shrink-0 text-sm text-text-muted">{label}</dt>
      <dd className="text-right text-sm">{value ?? '-'}</dd>
    </div>
  );
}

/**
 * 출석 그리드 행 클릭 시 열리는 회원 상세 패널.
 * lg 이상: 우측 고정 컬럼 / lg 미만: 화면 위로 덮는 슬라이드 패널.
 */
export function MemberDetailPanel({ memberId, onClose }: MemberDetailPanelProps) {
  const { data: member, isLoading, isError } = useMemberQuery(memberId);

  return (
    <>
      {/* lg 미만에서 뒷배경 클릭으로 닫기 */}
      <button
        type="button"
        aria-label="상세 패널 닫기"
        className="fixed inset-0 z-20 bg-black/30 lg:hidden"
        onClick={onClose}
      />

      <aside
        aria-label="회원 상세"
        className="fixed inset-y-0 right-0 z-30 w-80 overflow-y-auto border-l border-border bg-surface p-4 shadow-card lg:static lg:z-auto lg:h-fit lg:w-72 lg:shrink-0 lg:rounded-lg lg:border lg:shadow-none"
      >
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-base font-bold">회원 상세</h2>
          <button
            type="button"
            aria-label="닫기"
            className="rounded-md p-1 text-text-muted hover:bg-background hover:text-text-primary"
            onClick={onClose}
          >
            ✕
          </button>
        </div>

        {isLoading && <p className="py-8 text-center text-sm text-text-muted">불러오는 중...</p>}
        {isError && (
          <p className="py-8 text-center text-sm text-danger">
            회원 정보를 불러오지 못했습니다.
          </p>
        )}

        {member && (
          <div>
            <div className="mb-3 rounded-md bg-background p-3">
              <p className="font-bold">{member.name}</p>
              <p className="mt-0.5 text-sm text-text-muted">{member.memberNo}</p>
            </div>

            <dl className="divide-y divide-border">
              <DetailRow label="연락처" value={member.phone} />
              <DetailRow label="성별" value={member.gender} />
              <DetailRow label="생년월일" value={member.birthDate} />
              <DetailRow label="가입일" value={formatDate(member.createdAt)} />
            </dl>

            {/* 2차(이용권)·3차(PT권) 도메인 구현 후 실데이터로 채운다 */}
            <div className="mt-4 border-t border-border pt-3">
              <h3 className="text-sm font-semibold">이용권 · PT</h3>
              <p className="mt-2 text-sm text-text-muted">준비 중입니다.</p>
            </div>
          </div>
        )}
      </aside>
    </>
  );
}
