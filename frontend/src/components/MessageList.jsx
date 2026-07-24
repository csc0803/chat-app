import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import api from '../api';

const PAGE_SIZE = 20;

// roomId：目前房間；liveMessages：ChatRoom 透過 useWebSocket 收到的即時訊息（舊到新）
// 換房間時父層要帶 key={roomId} 讓這個元件整個 remount，分頁狀態才會乾淨重來
export default function MessageList({ roomId, liveMessages }) {
  const [history, setHistory] = useState([]); // 歷史訊息，舊到新排序
  const [loadingMore, setLoadingMore] = useState(false);
  const containerRef = useRef(null);
  const pageRef = useRef(0);
  const hasMoreRef = useRef(true);
  const loadingRef = useRef(false);
  const restoreRef = useRef(null); // 'bottom' | { prevScrollHeight } | null

  const loadPage = async () => {
    if (!roomId || loadingRef.current || !hasMoreRef.current) return;
    loadingRef.current = true;
    setLoadingMore(true);

    const isFirstPage = pageRef.current === 0;
    const container = containerRef.current;
    restoreRef.current = isFirstPage
      ? 'bottom'
      : { prevScrollHeight: container ? container.scrollHeight : 0 };

    try {
      const { data } = await api.get(`/rooms/${roomId}/messages`, {
        params: { page: pageRef.current, size: PAGE_SIZE },
      });
      const olderChunk = [...data.content].reverse(); // 後端回 DESC（新到舊），反轉成舊到新
      setHistory((prev) => [...olderChunk, ...prev]);
      hasMoreRef.current = !data.last;
      pageRef.current += 1;
    } finally {
      loadingRef.current = false;
      setLoadingMore(false);
    }
  };

  // 掛載時載入第一頁（最新 20 則）；換房間靠父層的 key={roomId} remount 來重置
  useEffect(() => {
    if (roomId) loadPage();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId]);

  // 首次載入捲到最底；載入更舊頁時維持原本的捲動位置（不跳動）
  useLayoutEffect(() => {
    const container = containerRef.current;
    if (!container || !restoreRef.current) return;

    if (restoreRef.current === 'bottom') {
      container.scrollTop = container.scrollHeight;
    } else {
      container.scrollTop += container.scrollHeight - restoreRef.current.prevScrollHeight;
    }
    restoreRef.current = null;
  }, [history]);

  const handleScroll = () => {
    if (containerRef.current && containerRef.current.scrollTop < 50) {
      loadPage();
    }
  };

  const messages = [...history, ...liveMessages];

  return (
    <div
      ref={containerRef}
      onScroll={handleScroll}
      style={{ height: 400, overflowY: 'auto', border: '1px solid #ccc' }}
    >
      {loadingMore && <p>載入中...</p>}
      <ul>
        {messages.map((msg, i) => (
          <li key={`${msg.id ?? msg.sentAt}-${i}`}>
            <strong>{msg.sender}</strong>：{msg.content}
          </li>
        ))}
      </ul>
    </div>
  );
}
