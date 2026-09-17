import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Client } from '@stomp/stompjs';
import { apiClient } from '@/shared/api/client';
import useUser from '@/shared/hooks/useUser';
import './ChatRoom.css';

interface Message {
  messageId: string;
  senderId: string;
  senderName: string;
  content: string;
  createdAt: string;
}

interface ChatRoomProps {
  roomId: string;
  title?: string;
}

export default function ChatRoom({ roomId, title }: ChatRoomProps) {
  const { t } = useTranslation();
  const { data: user } = useUser();
  const [messages, setMessages] = useState<Message[]>([]);
  const [content, setContent] = useState('');
  const [error, setError] = useState('');

  const refresh = async () => {
    const r = await apiClient.get<{ data: Message[] }>(`/chat/rooms/${roomId}/messages`);
    if (r.success) {
      setMessages(r.data.data);
    } else {
      setError(t('chat.loadFailed'));
    }
  };

  useEffect(() => {
    void refresh();
    const api = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    const wsUrl = api.replace(/^http/, 'ws') + '/ws/chat-native';
    const stomp = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 3000,
      onConnect: () => {
        stomp.subscribe(`/topic/chat/${roomId}`, (packet) => {
          setMessages((current) => [...current, JSON.parse(packet.body) as Message]);
        });
      },
    });
    stomp.activate();
    return () => { void stomp.deactivate(); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId]);

  const send = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!content.trim()) return;
    const r = await apiClient.post(`/chat/rooms/${roomId}/messages`, { content, title });
    if (r.success) {
      setContent('');
      setError('');
    } else {
      setError(t('chat.sendFailed'));
    }
  };

  return (
    <div className="chat-room">
      <div className="chat-messages">
        {messages.map((m) => (
          <article key={m.messageId} className={m.senderId === user?.userId ? 'mine' : ''}>
            <b>{m.senderId === user?.userId ? t('chat.me') : m.senderName}</b>
            <p>{m.content}</p>
          </article>
        ))}
      </div>
      {error && <p className="chat-room-error" role="alert">{error}</p>}
      <form onSubmit={send}>
        <input value={content} onChange={(e) => setContent(e.target.value)} maxLength={1000} placeholder={t('chat.placeholder')} aria-label={t('chat.placeholder')} />
        <button type="submit" className="chat-send-btn" aria-label={t('chat.send')}>➤</button>
      </form>
    </div>
  );
}
