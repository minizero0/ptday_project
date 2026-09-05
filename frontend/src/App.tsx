import { Navigate, Route, Routes } from 'react-router-dom';
import { AdminLayout } from './components/layout/AdminLayout';
import { AttendancePage } from './features/attendance/pages/AttendancePage';
import { LoginPage } from './features/auth/pages/LoginPage';
import { useAuthStore } from './features/auth/store/authStore';

// 아직 구현 전인 도메인 페이지 자리 표시
function PlaceholderPage({ title }: { title: string }) {
  return (
    <div>
      <h1 className="text-xl font-bold">{title}</h1>
      <p className="mt-2 text-sm text-text-muted">준비 중인 화면입니다.</p>
    </div>
  );
}

function App() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  return (
    <Routes>
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />}
      />
      <Route
        element={isAuthenticated ? <AdminLayout /> : <Navigate to="/login" replace />}
      >
        <Route path="/" element={<AttendancePage />} />
        <Route path="/members" element={<PlaceholderPage title="회원관리" />} />
        <Route path="/attendance" element={<AttendancePage />} />
        <Route path="/payments" element={<PlaceholderPage title="결제관리" />} />
        <Route path="/memberships" element={<PlaceholderPage title="이용권관리" />} />
        <Route path="/pt-passes" element={<PlaceholderPage title="PT권관리" />} />
        <Route path="/pt-reservations" element={<PlaceholderPage title="PT예약" />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
