import { Button } from './components/Button';
import { Card } from './components/Card';
import { LoginPage } from './features/auth/pages/LoginPage';
import { useAuthStore } from './features/auth/store/authStore';

function App() {
  const { isAuthenticated, username, role, logout } = useAuthStore();

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  // 로그인 후 임시 확인 화면 — 다음 단계에서 대시보드/회원 목록으로 대체
  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-sm text-center">
        <h1 className="text-xl font-bold">로그인 성공 🎉</h1>
        <p className="mt-2 text-sm text-text-muted">
          {username ? `${username} · ` : ''}
          {role}
        </p>
        <Button variant="ghost" className="mt-4" onClick={logout}>
          로그아웃
        </Button>
      </Card>
    </div>
  );
}

export default App;
