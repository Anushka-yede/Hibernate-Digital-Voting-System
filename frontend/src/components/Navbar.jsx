import { Link, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { authApi } from '../services/api';
import { getRefreshToken } from '../services/session';

export default function Navbar({ user, onLogout }) {
  const navigate = useNavigate();
  const [theme, setTheme] = useState(localStorage.getItem('theme') || 'light');

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);

  const handleLogout = async () => {
    const refreshToken = getRefreshToken();
    if (refreshToken) {
      try {
        await authApi.logout(refreshToken);
      } catch {
        // Ignore logout API failure and clear local session anyway.
      }
    }
    onLogout();
    navigate('/login');
  };

  return (
    <header className="nav-shell">
      <div className="container nav-content">
        <Link className="brand" to="/">SecureVote</Link>
        <nav className="nav-links">
          <Link to="/">Home</Link>
          <Link to="/candidates">Candidates</Link>
          {!user && <Link to="/login">Login</Link>}
          {!user && <Link to="/register">Register</Link>}
          {user?.role === 'ADMIN' && <Link to="/admin">Admin</Link>}
          {user?.role === 'USER' && <Link to="/dashboard">Dashboard</Link>}
          <button type="button" className="secondary-btn" onClick={() => setTheme((prev) => (prev === 'light' ? 'dark' : 'light'))}>
            {theme === 'light' ? 'Dark Theme' : 'Light Theme'}
          </button>
          {user && <span className="user-chip">{user.username} ({user.role})</span>}
          {user && <button className="ghost-btn" onClick={handleLogout}>Logout</button>}
        </nav>
      </div>
    </header>
  );
}
