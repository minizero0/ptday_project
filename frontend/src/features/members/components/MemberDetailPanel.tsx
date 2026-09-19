import { useState } from 'react';
import { Button } from '../../../components/Button';
import { cn } from '../../../lib/cn';
import { MembershipCreateModal } from '../../memberships/components/MembershipCreateModal';
import { MembershipHistory } from '../../memberships/components/MembershipHistory';
import { MembershipSummary } from '../../memberships/components/MembershipSummary';
import { PtPassSection } from '../../pt-passes/components/PtPassSection';
import { useMemberQuery } from '../hooks/useMember';

interface MemberDetailPanelProps {
  memberId: number | null;
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
 * 회원 상세 패널.
 * lg 이상: 항상 우측에 고정 표시 (미선택 시 안내 문구).
 * lg 미만: 행을 선택했을 때만 화면 위로 덮는 슬라이드 패널.
 */
export function MemberDetailPanel({ memberId, onClose }: MemberDetailPanelProps) {
  const { data: member, isLoading, isError } = useMemberQuery(memberId);
  const [isCreateMembershipOpen, setIsCreateMembershipOpen] = useState(false);

  const isSelected = memberId !== null;

  return (
    <>
      {/* lg 미만에서 뒷배경 클릭으로 닫기 */}
      {isSelected && (
        <button
          type="button"
          aria-label="상세 패널 닫기"
          className="fixed inset-0 z-20 bg-black/30 lg:hidden"
          onClick={onClose}
        />
      )}

      <aside
        aria-label="회원 상세"
        className={cn(
          'w-80 shrink-0 overflow-y-auto border-border bg-surface p-4',
          'lg:static lg:z-auto lg:block lg:h-full lg:rounded-lg lg:border lg:shadow-none xl:w-96',
          isSelected
            ? 'fixed inset-y-0 right-0 z-30 border-l shadow-card'
            : 'hidden',
        )}
      >
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-base font-bold">회원 상세</h2>
          {isSelected && (
            <button
              type="button"
              aria-label="닫기"
              className="rounded-md p-1 text-text-muted hover:bg-background hover:text-text-primary"
              onClick={onClose}
            >
              ✕
            </button>
          )}
        </div>

        {!isSelected && (
          <div className="flex h-64 items-center justify-center">
            <p className="text-center text-sm text-text-muted">
              목록에서 행을 클릭하면
              <br />
              회원 정보가 표시됩니다.
            </p>
          </div>
        )}

        {isSelected && isLoading && (
          <p className="py-8 text-center text-sm text-text-muted">불러오는 중...</p>
        )}
        {isSelected && isError && (
          <p className="py-8 text-center text-sm text-danger">회원 정보를 불러오지 못했습니다.</p>
        )}

        {isSelected && member && (
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

            <MembershipSummary memberId={member.id} />

            <div className="mt-3 border-t border-border pt-3">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold">이용권 이력</h3>
                <Button size="sm" onClick={() => setIsCreateMembershipOpen(true)}>
                  이용권 등록
                </Button>
              </div>
              <MembershipHistory member={member} />
            </div>
            <PtPassSection member={member} />
          </div>
        )}
      </aside>

      {isCreateMembershipOpen && member && (
        <MembershipCreateModal member={member} onClose={() => setIsCreateMembershipOpen(false)} />
      )}
    </>
  );
}
