import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// roomId, token 任一為 null 時不連線（例如尚未登入 / 還沒選房間）
export function useWebSocket(roomId, token) {
  const clientRef = useRef(null);
  const subscriptionRef = useRef(null);
  const [connected, setConnected] = useState(false);
  const [messages, setMessages] = useState([]);

  useEffect(() => {
    if (!roomId || !token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(import.meta.env.VITE_WS_URL),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000, // 斷線後 5 秒自動重連

      onConnect: () => {
        setConnected(true);
        subscriptionRef.current = client.subscribe(
          `/topic/room.${roomId}`,
          (frame) => {
            setMessages((prev) => [...prev, JSON.parse(frame.body)]);
          }
        );
      },

      onWebSocketClose: () => {
        setConnected(false);
      },

      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers?.message, frame.body);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      subscriptionRef.current?.unsubscribe();
      client.deactivate();
      clientRef.current = null;
    };
  }, [roomId, token]);

  const sendMessage = useCallback(
    (content, type = 'CHAT') => {
      if (!clientRef.current?.connected) return;
      clientRef.current.publish({
        destination: '/app/chat.send',
        body: JSON.stringify({ roomId, content, type }),
      });
    },
    [roomId]
  );

  return { connected, messages, sendMessage };
}
