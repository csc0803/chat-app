import { useParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useWebSocket } from '../hooks/useWebSocket';
import MessageList from '../components/MessageList';
import MessageInput from '../components/MessageInput';
import UserList from '../components/UserList';

export default function ChatRoom() {
  const { roomId } = useParams();
  const { token } = useAuth();
  const { connected, messages, sendMessage } = useWebSocket(roomId, token);

  return (
    <div style={{ display: 'flex', gap: '1rem' }}>
      <div style={{ flex: 1 }}>
        <h1>
          房間 #{roomId}（{connected ? '已連線' : '連線中...'}）
        </h1>
        <MessageList messages={messages} />
        <MessageInput onSend={sendMessage} disabled={!connected} />
      </div>
      <UserList roomId={roomId} />
    </div>
  );
}
