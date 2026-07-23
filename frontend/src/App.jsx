import { Navigate, Route, Routes } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/Register';
import RoomList from './pages/RoomList';
import ChatRoom from './pages/ChatRoom';
import { useAuth } from './hooks/useAuth';

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
            <ChatRoom />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

export default App;
