import { Navigate, Route, Routes } from 'react-router-dom';
import { AdminLayout } from './components/layout/AdminLayout';
import { AttendancePage } from './features/attendance/pages/AttendancePage';
import { KioskPage } from './features/attendance/pages/KioskPage';
import { LoginPage } from './features/auth/pages/LoginPage';
import { MembersPage } from './features/members/pages/MembersPage';
import { MembershipsPage } from './features/memberships/pages/MembershipsPage';
import { useAuthStore } from './features/auth/store/authStore';
import { ROLE_KIOSK } from './features/auth/types/auth';
import { PtPassesPage } from './features/pt-passes/pages/PtPassesPage';
import { PtReservationsPage } from './features/pt-reservations/pages/PtReservationsPage';

// 아직 구현 전인 도메인 페이지 자리 표시
function PlaceholderPage({ title }: { title: string }) {
  return (
    <div>
      <h1 className="text-xl font-bold">{title}</h1>
      <p className="mt-2 text-sm text-text-muted">준비 중인 화면입니다.</p>
    </div>
  );
}

interface AdminRouteProps {
  isAuthenticated: boolean;
  isKioskAccount: boolean;
}

function AdminRoute({ isAuthenticated, isKioskAccount }: AdminRouteProps) {
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  if (isKioskAccount) {
    return <Navigate to="/kiosk" replace />;
  }
  return <AdminLayout />;
}

function App() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  // 입구 키오스크 계정은 출석 화면만 쓴다. 관리 화면 주소로 들어와도 출석 화면으로 돌려보낸다.
  // (화면 제어일 뿐이고, 실제 접근 차단은 서버 권한 규칙이 한다)
  const isKioskAccount = useAuthStore((state) => state.role) === ROLE_KIOSK;
  const homePath = isKioskAccount ? '/kiosk' : '/';

  return (
    <Routes>
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to={homePath} replace /> : <LoginPage />}
      />
      <Route
        path="/kiosk"
        element={isAuthenticated ? <KioskPage /> : <Navigate to="/login" replace />}
      />
      <Route element={<AdminRoute isAuthenticated={isAuthenticated} isKioskAccount={isKioskAccount} />}>
        <Route path="/" element={<AttendancePage />} />
        <Route path="/members" element={<MembersPage />} />
        <Route path="/attendance" element={<AttendancePage />} />
        <Route path="/payments" element={<PlaceholderPage title="결제관리" />} />
        <Route path="/memberships" element={<MembershipsPage />} />
        <Route path="/pt-passes" element={<PtPassesPage />} />
        <Route path="/pt-reservations" element={<PtReservationsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
