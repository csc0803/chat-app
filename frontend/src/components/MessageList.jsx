export default function MessageList({ messages }) {
  return (
    <ul>
      {messages.map((msg, i) => (
        <li key={`${msg.sentAt}-${i}`}>
          <strong>{msg.sender}</strong>：{msg.content}
        </li>
      ))}
    </ul>
  );
}
