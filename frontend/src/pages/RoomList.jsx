import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';

export default function RoomList() {
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [newRoomName, setNewRoomName] = useState('');
  const [creating, setCreating] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    let cancelled = false;

    api
      .get('/rooms')
      .then(({ data }) => {
        if (cancelled) return;
        setRooms(data);
        setError('');
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err.response?.data || '房間列表載入失敗');
      })
      .finally(() => {
        if (cancelled) return;
        setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const handleCreate = async (e) => {
    e.preventDefault();
    setError('');
    setCreating(true);
    try {
      const { data: room } = await api.post('/rooms', { name: newRoomName });
      navigate(`/rooms/${room.id}`);
    } catch (err) {
      setError(err.response?.data || '建立房間失敗');
    } finally {
      setCreating(false);
    }
  };

  return (
    <div>
      <h1>房間列表</h1>

      <form onSubmit={handleCreate}>
        <input
          value={newRoomName}
          onChange={(e) => setNewRoomName(e.target.value)}
          placeholder="房間名稱"
          required
        />
        <button type="submit" disabled={creating}>
          建立房間
        </button>
      </form>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      {loading ? (
        <p>載入中...</p>
      ) : rooms.length === 0 ? (
        <p>目前沒有房間</p>
      ) : (
        <ul>
          {rooms.map((room) => (
            <li key={room.id}>
              <button type="button" onClick={() => navigate(`/rooms/${room.id}`)}>
                {room.name}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
