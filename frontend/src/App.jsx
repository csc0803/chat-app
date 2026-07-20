import { Navigate, Route, Routes, useParams } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/Register';
import RoomList from './pages/RoomList';
import { useAuth } from './hooks/useAuth';

function ChatRoomPlaceholder() {
  const { roomId } = useParams();
  return <div>聊天室 #{roomId}（T33 尚未實作）</div>;
}

function ProtectedRoute({ children }) {
  const { isAuthed } = useAuth();
  return isAuthed ? children : <Navigate to="/login" replace />;
}

function App() {
  const { isAuthed } = useAuth();

  return (
    <Routes>
      <Route path="/" element={<Navigate to={isAuthed ? '/rooms' : '/login'} replace />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route
        path="/rooms"
        element={
          <ProtectedRoute>
            <RoomList />
          </ProtectedRoute>
        }
      />
      <Route
        path="/rooms/:roomId"
        element={
          <ProtectedRoute>
            <ChatRoomPlaceholder />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

export default App;
