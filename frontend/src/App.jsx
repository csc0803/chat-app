import { Navigate, Route, Routes } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/Register';
import { useAuth } from './hooks/useAuth';

function RoomListPlaceholder() {
  return <div>房間列表（T31 尚未實作）</div>;
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
            <RoomListPlaceholder />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

export default App;
