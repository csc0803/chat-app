import { useState } from 'react';

export default function MessageInput({ onSend, disabled }) {
  const [content, setContent] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    const trimmed = content.trim();
    if (!trimmed) return;
    onSend(trimmed);
    setContent('');
  };

  return (
    <form onSubmit={handleSubmit}>
      <input
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="輸入訊息..."
        disabled={disabled}
      />
      <button type="submit" disabled={disabled}>
        送出
      </button>
    </form>
  );
}
