import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import api from '../api';
import { useAuth } from '../hooks/useAuth';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const { data: token } = await api.post('/auth/login', { username, password });
      login(token);
      navigate('/rooms', { replace: true });
    } catch (err) {
      setError(err.response?.data || '登入失敗');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <h1>登入</h1>
      {location.state?.registered && <p>註冊成功，請登入</p>}
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <label>
        帳號
        <input value={username} onChange={(e) => setUsername(e.target.value)} required />
      </label>
      <label>
        密碼
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
      </label>
      <button type="submit" disabled={submitting}>
        登入
      </button>
      <p>
        還沒有帳號？<Link to="/register">註冊</Link>
      </p>
    </form>
  );
}
