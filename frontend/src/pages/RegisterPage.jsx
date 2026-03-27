import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { authApi } from '../services/api';
import BackHomeButton from '../components/BackHomeButton';
import { useAlert } from '../hooks/useAlert';

function scorePassword(password) {
  let score = 0;
  if (password.length >= 8) score += 1;
  if (/[A-Z]/.test(password)) score += 1;
  if (/[a-z]/.test(password)) score += 1;
  if (/[0-9]/.test(password)) score += 1;
  if (/[^A-Za-z0-9]/.test(password)) score += 1;
  return score;
}

export default function RegisterPage({ onLogin, currentUser }) {
  const navigate = useNavigate();
  const { pushAlert } = useAlert();
  const [form, setForm] = useState({ username: '', email: '', password: '', dateOfBirth: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const strength = useMemo(() => scorePassword(form.password), [form.password]);

  const calculateAge = (dob) => {
    if (!dob) return 0;
    const birth = new Date(dob);
    const now = new Date();
    let age = now.getFullYear() - birth.getFullYear();
    const monthDelta = now.getMonth() - birth.getMonth();
    if (monthDelta < 0 || (monthDelta === 0 && now.getDate() < birth.getDate())) {
      age -= 1;
    }
    return age;
  };

  const submit = async (e) => {
    e.preventDefault();
    setError('');

    if (!form.username || !form.email || !form.password || !form.dateOfBirth) {
      setError('All fields are required.');
      return;
    }

    if (calculateAge(form.dateOfBirth) < 18) {
      setError('You must be at least 18 years old to register.');
      return;
    }

    try {
      setLoading(true);
      const { data } = await authApi.register(form);
      onLogin(data);
      pushAlert('Account created successfully. You are now signed in.', 'success');
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed');
      pushAlert('Unable to create account. Please check your details.', 'error');
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
        <h2>Create Account</h2>
        <p>Join the secure digital election platform in under a minute.</p>
        {error && <div className="error-box">{error}</div>}
        <label className={`field ${form.username ? 'filled' : ''}`}>
          <span>Username</span>
          <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
        </label>
        <label className={`field ${form.email ? 'filled' : ''}`}>
          <span>Email</span>
          <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </label>
        <label className={`field ${form.password ? 'filled' : ''}`}>
          <span>Password</span>
          <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </label>
        <label className={`field ${form.dateOfBirth ? 'filled' : ''}`}>
          <span>Date of Birth</span>
          <input type="date" value={form.dateOfBirth} onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })} />
        </label>

        <div className="strength-bar">
          <span style={{ width: `${(strength / 5) * 100}%` }} />
        </div>

        <button className="primary-btn" type="submit" disabled={loading}>
          {loading ? 'Creating account...' : 'Register'}
        </button>
        <div className="inline-actions">
          <span>Already registered?</span>
          <Link className="link-btn" to="/login">Sign in</Link>
        </div>
        <div className="panel mini-panel">
          <strong>Role assignment</strong>
          <p>New registrations are created as USER. Admin access is controlled by backend role management.</p>
        </div>
        <BackHomeButton />
      </motion.form>
    </div>
  );
}
