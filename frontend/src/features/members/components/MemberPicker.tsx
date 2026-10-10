import { useState } from 'react';
import { Button } from '../../../components/Button';
import { Input } from '../../../components/Input';
import { useDebounce } from '../../../hooks/useDebounce';
import { useMemberPageQuery, useMemberSearchQuery } from '../hooks/useMember';
import type { Member } from '../types/member';

interface MemberPickerProps {
  onSelect: (member: Member) => void;
}

type PickerTab = 'search' | 'all';

const TABS: { id: PickerTab; label: string }[] = [
  { id: 'search', label: '검색' },
  { id: 'all', label: '전체 회원' },
];
const SEARCH_DEBOUNCE_MS = 250;
const LIST_CLASS_NAME = 'max-h-40 overflow-y-auto rounded-md border border-border';
const MESSAGE_CLASS_NAME = 'px-3 py-4 text-center text-sm';

function MemberOption({ member, onSelect }: { member: Member } & MemberPickerProps) {
  return (
    <button
      type="button"
      onClick={() => onSelect(member)}
      className="flex w-full items-center justify-between border-b border-border px-3 py-2 text-left last:border-b-0 hover:bg-background"
    >
      <span className="text-sm font-medium">{member.name}</span>
      <span className="text-xs text-text-muted">
        {member.memberNo} · {member.phone ?? '-'}
      </span>
    </button>
  );
}

function SearchPanel({ onSelect }: MemberPickerProps) {
  const [keyword, setKeyword] = useState('');
  const debouncedKeyword = useDebounce(keyword, SEARCH_DEBOUNCE_MS);
  const { data: searchResult, isFetching, isError } = useMemberSearchQuery(debouncedKeyword);

  const hasKeyword = keyword.trim().length > 0;

  return (
    <>
      <Input
        label="회원 검색"
        placeholder="이름, 회원번호, 전화번호"
        value={keyword}
        onChange={(event) => setKeyword(event.target.value)}
      />
      <div className={`mt-2 ${LIST_CLASS_NAME}`}>
        {!hasKeyword && (
          <p className={`${MESSAGE_CLASS_NAME} text-text-muted`}>검색어를 입력하세요.</p>
        )}
        {hasKeyword && isFetching && (
          <p className={`${MESSAGE_CLASS_NAME} text-text-muted`}>검색 중...</p>
        )}
        {hasKeyword && !isFetching && isError && (
          <p className={`${MESSAGE_CLASS_NAME} text-danger`}>회원을 검색하지 못했습니다.</p>
        )}
        {hasKeyword && !isFetching && searchResult?.content.length === 0 && (
          <p className={`${MESSAGE_CLASS_NAME} text-text-muted`}>검색 결과가 없습니다.</p>
        )}
        {hasKeyword &&
          !isFetching &&
          searchResult?.content.map((member) => (
            <MemberOption key={member.id} member={member} onSelect={onSelect} />
          ))}
      </div>
    </>
  );
}

/** 검색어가 떠오르지 않을 때 훑어보는 전체 목록. 회원관리 목록과 같은 순서(최근 등록순)다. */
function AllMembersPanel({ onSelect }: MemberPickerProps) {
  const [page, setPage] = useState(0);
  const { data, isPending, isPaused, isError } = useMemberPageQuery('', page);

  return (
    <>
      <div className={LIST_CLASS_NAME}>
        {isPending && (
          <p className={`${MESSAGE_CLASS_NAME} text-text-muted`}>
            {isPaused ? '네트워크 연결을 확인해주세요.' : '불러오는 중...'}
          </p>
        )}
        {isError && (
          <p className={`${MESSAGE_CLASS_NAME} text-danger`}>회원 목록을 불러오지 못했습니다.</p>
        )}
        {data && data.content.length === 0 && (
          <p className={`${MESSAGE_CLASS_NAME} text-text-muted`}>등록된 회원이 없습니다.</p>
        )}
        {data?.content.map((member) => (
          <MemberOption key={member.id} member={member} onSelect={onSelect} />
        ))}
      </div>
      {data && data.totalPages > 1 && (
        <div className="mt-2 flex items-center justify-center gap-3 text-xs">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            disabled={data.first}
            onClick={() => setPage((current) => current - 1)}
          >
            이전
          </Button>
          <span className="text-text-muted">
            {data.page + 1} / {data.totalPages} · 총 {data.totalElements}명
          </span>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            disabled={data.last}
            onClick={() => setPage((current) => current + 1)}
          >
            다음
          </Button>
        </div>
      )}
    </>
  );
}

/**
 * 회원 한 명을 고른다. 이름·회원번호·전화번호로 찾거나, 전체 목록에서 훑어 고른다.
 * 고른 뒤의 표시는 부르는 쪽이 맡는다.
 */
export function MemberPicker({ onSelect }: MemberPickerProps) {
  const [activeTab, setActiveTab] = useState<PickerTab>('search');

  return (
    <div>
      <div role="tablist" aria-label="회원 선택 방법" className="mb-3 flex border-b border-border">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            type="button"
            role="tab"
            id={`member-picker-tab-${tab.id}`}
            aria-selected={activeTab === tab.id}
            aria-controls="member-picker-panel"
            onClick={() => setActiveTab(tab.id)}
            className={`-mb-px border-b-2 px-3 py-2 text-sm ${
              activeTab === tab.id
                ? 'border-primary font-semibold text-primary'
                : 'border-transparent text-text-muted hover:text-text-primary'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>
      <div
        role="tabpanel"
        id="member-picker-panel"
        aria-labelledby={`member-picker-tab-${activeTab}`}
      >
        {activeTab === 'search' ? (
          <SearchPanel onSelect={onSelect} />
        ) : (
          <AllMembersPanel onSelect={onSelect} />
        )}
      </div>
    </div>
  );
}
