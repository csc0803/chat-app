// 在線名單由 ChatRoom 透過 useWebSocket 訂閱 /topic/room.{roomId}.users 取得
export default function UserList({ users }) {
  return (
    <aside>
      <h2>在線人數（{users.length}）</h2>
      <ul>
        {users.map((username) => (
          <li key={username}>{username}</li>
        ))}
      </ul>
    </aside>
  );
}
