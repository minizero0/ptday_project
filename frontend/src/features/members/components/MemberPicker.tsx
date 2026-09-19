import { useState } from 'react';
import { Input } from '../../../components/Input';
import { useDebounce } from '../../../hooks/useDebounce';
import { useMemberSearchQuery } from '../hooks/useMember';
import type { Member } from '../types/member';

interface MemberPickerProps {
  onSelect: (member: Member) => void;
}

const SEARCH_DEBOUNCE_MS = 250;

/** 이름·회원번호·전화번호로 회원을 찾아 한 명을 고른다. 고른 뒤의 표시는 부르는 쪽이 맡는다. */
export function MemberPicker({ onSelect }: MemberPickerProps) {
  const [keyword, setKeyword] = useState('');
  const debouncedKeyword = useDebounce(keyword, SEARCH_DEBOUNCE_MS);
  const { data: searchResult, isFetching, isError } = useMemberSearchQuery(debouncedKeyword);

  const hasKeyword = keyword.trim().length > 0;

  return (
    <div>
      <Input
        label="회원 검색"
        placeholder="이름, 회원번호, 전화번호"
        value={keyword}
        onChange={(event) => setKeyword(event.target.value)}
      />
      <div className="mt-2 max-h-40 overflow-y-auto rounded-md border border-border">
        {!hasKeyword && (
          <p className="px-3 py-4 text-center text-sm text-text-muted">검색어를 입력하세요.</p>
        )}
        {hasKeyword && isFetching && (
          <p className="px-3 py-4 text-center text-sm text-text-muted">검색 중...</p>
        )}
        {hasKeyword && !isFetching && isError && (
          <p className="px-3 py-4 text-center text-sm text-danger">회원을 검색하지 못했습니다.</p>
        )}
        {hasKeyword && !isFetching && searchResult?.content.length === 0 && (
          <p className="px-3 py-4 text-center text-sm text-text-muted">검색 결과가 없습니다.</p>
        )}
        {hasKeyword &&
          !isFetching &&
          searchResult?.content.map((member) => (
            <button
              key={member.id}
              type="button"
              onClick={() => onSelect(member)}
              className="flex w-full items-center justify-between border-b border-border px-3 py-2 text-left last:border-b-0 hover:bg-background"
            >
              <span className="text-sm font-medium">{member.name}</span>
              <span className="text-xs text-text-muted">
                {member.memberNo} · {member.phone ?? '-'}
              </span>
            </button>
          ))}
      </div>
    </div>
  );
}
