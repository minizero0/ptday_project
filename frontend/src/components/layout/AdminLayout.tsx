import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../features/auth/store/authStore';
import { Button } from '../Button';
import { cn } from '../../lib/cn';

const NAV_ITEMS = [
  { to: '/members', label: '회원관리' },
  { to: '/attendance', label: '출석관리' },
  { to: '/payments', label: '결제관리' },
  { to: '/memberships', label: '이용권관리' },
  { to: '/pt-passes', label: 'PT권관리' },
  { to: '/pt-reservations', label: 'PT예약' },
] as const;

export function AdminLayout() {
  const { username, role, logout } = useAuthStore();
  // 태블릿 이하에서 사이드바 열림/닫힘 (lg 이상은 항상 고정 표시)
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  const closeSidebar = () => setIsSidebarOpen(false);

  return (
    <div className="min-h-screen bg-background">
      {/* 모바일/태블릿에서 사이드바 열렸을 때 뒷배경 클릭으로 닫기 */}
      {isSidebarOpen && (
        <button
          type="button"
          aria-label="사이드바 닫기"
          className="fixed inset-0 z-20 bg-black/30 lg:hidden"
          onClick={closeSidebar}
        />
      )}

      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-30 flex w-60 flex-col border-r border-border bg-surface transition-transform lg:translate-x-0',
          isSidebarOpen ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex h-14 items-center border-b border-border px-4">
          <NavLink to="/" className="text-lg font-bold" onClick={closeSidebar}>
            ptday <span className="text-primary">관리자</span>
          </NavLink>
        </div>

        <nav aria-label="주요 메뉴" className="flex-1 space-y-1 p-2">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={closeSidebar}
              className={({ isActive }) =>
                cn(
                  'block rounded-md px-3 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-primary/10 text-text-primary'
                    : 'text-text-muted hover:bg-background hover:text-text-primary',
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      {/* h-screen 으로 높이를 확정해야 내부 h-full(상세 패널 꽉 채움)이 동작한다. 넘치는 내용은 main 에서 스크롤 */}
      <div className="flex h-screen flex-col lg:pl-60">
        <header className="sticky top-0 z-10 flex h-14 items-center justify-between border-b border-border bg-surface px-4">
          <button
            type="button"
            aria-label="사이드바 열기"
            className="rounded-md p-2 text-text-muted hover:bg-background lg:hidden"
            onClick={() => setIsSidebarOpen(true)}
          >
            <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path
                d="M4 6h16M4 12h16M4 18h16"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
              />
            </svg>
          </button>

          <div className="ml-auto flex items-center gap-3">
            <span className="text-sm text-text-muted">
              {username ? `${username} · ` : ''}
              {role}
            </span>
            <Button variant="ghost" size="sm" onClick={logout}>
              로그아웃
            </Button>
          </div>
        </header>

        <main className="min-h-0 flex-1 overflow-y-auto p-4 lg:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
