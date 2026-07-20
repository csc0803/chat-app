import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api';

export default function Register() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await api.post('/auth/register', { username, password });
      navigate('/login', { state: { registered: true } });
    } catch (err) {
      setError(err.response?.data || '註冊失敗');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <h1>註冊</h1>
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
        註冊
      </button>
      <p>
        已經有帳號？<Link to="/login">登入</Link>
      </p>
    </form>
  );
}
