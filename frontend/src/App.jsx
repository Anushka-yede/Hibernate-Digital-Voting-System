import { Navigate, Route, Routes } from 'react-router-dom';
import { Suspense, lazy, useState } from 'react';
import Navbar from './components/Navbar';
import { clearSession, getSession, setSession } from './services/session';
import SkeletonGrid from './components/SkeletonGrid';

const HomePage = lazy(() => import('./pages/HomePage'));
const LoginPage = lazy(() => import('./pages/LoginPage'));
const RegisterPage = lazy(() => import('./pages/RegisterPage'));
const UserDashboard = lazy(() => import('./pages/UserDashboard'));
const AdminDashboard = lazy(() => import('./pages/AdminDashboard'));
const CandidateProfilePage = lazy(() => import('./pages/CandidateProfilePage'));

function getInitialUser() {
  return getSession()?.user || null;
}

function RoleGuard({ user, allow, children }) {
  if (!user) return <Navigate to="/login" replace />;
  if (!allow.includes(user.role)) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  const [user, setUser] = useState(getInitialUser());

  const handleLogin = (authResponse) => {
    setSession(authResponse);
    setUser(getSession()?.user || null);
  };

  const logout = () => {
    clearSession();
    setUser(null);
  };

  return (
    <>
      <Navbar user={user} onLogout={logout} />
      <Suspense fallback={<div className="container page-space"><SkeletonGrid count={3} /></div>}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage onLogin={handleLogin} currentUser={user} />} />
          <Route path="/register" element={<RegisterPage onLogin={handleLogin} currentUser={user} />} />
          <Route
            path="/dashboard"
            element={
              <RoleGuard user={user} allow={['USER']}>
                <UserDashboard />
              </RoleGuard>
            }
          />
          <Route
            path="/admin"
            element={
              <RoleGuard user={user} allow={['ADMIN']}>
                <AdminDashboard />
              </RoleGuard>
            }
          />
          <Route path="/candidates" element={<CandidateProfilePage />} />
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </Suspense>
    </>
  );
}
