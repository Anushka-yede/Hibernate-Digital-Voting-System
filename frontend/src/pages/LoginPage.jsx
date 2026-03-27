import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { authApi } from '../services/api';
import BackHomeButton from '../components/BackHomeButton';
import { useAlert } from '../hooks/useAlert';

export default function LoginPage({ onLogin, currentUser }) {
  const navigate = useNavigate();
  const { pushAlert } = useAlert();
  const [form, setForm] = useState({ usernameOrEmail: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');

    if (!form.usernameOrEmail || !form.password) {
      setError('Both fields are required.');
      return;
    }

    try {
      setLoading(true);
      const { data } = await authApi.login(form);
      onLogin(data);
      pushAlert(`Welcome ${data.username}. Redirecting to your dashboard.`, 'success');
      navigate(data.role === 'ADMIN' ? '/admin' : '/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
      pushAlert('Unable to sign in with provided credentials.', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="centered-page">
      <motion.form
        className="auth-card auth-premium"
        onSubmit={submit}
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
      >
        <h2>Welcome back</h2>
        <p>Access your secure election workspace.</p>
        {currentUser && (
          <div className="info-box">
            You are currently signed in as {currentUser.username} ({currentUser.role}).
            Logging in again will switch your active session.
          </div>
        )}
        {error && <div className="error-box">{error}</div>}
        <label className={`field ${form.usernameOrEmail ? 'filled' : ''}`}>
          <span>Username or Email</span>
          <input value={form.usernameOrEmail} onChange={(e) => setForm({ ...form, usernameOrEmail: e.target.value })} />
        </label>
        <label className={`field ${form.password ? 'filled' : ''}`}>
          <span>Password</span>
          <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </label>
        <button className="primary-btn" type="submit" disabled={loading}>
          {loading ? 'Signing in...' : 'Login'}
        </button>
        <div className="inline-actions">
          <span>New here?</span>
          <Link className="link-btn" to="/register">Create account</Link>
        </div>
        <div className="panel mini-panel">
          <strong>Role-aware routing</strong>
          <p>Admin credentials open the Admin Dashboard and voter credentials open the User Dashboard.</p>
          <p>Demo admin: <strong>admin</strong> / <strong>Admin@123</strong></p>
        </div>
        <BackHomeButton />
      </motion.form>
    </div>
  );
}
