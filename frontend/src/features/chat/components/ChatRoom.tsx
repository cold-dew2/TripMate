import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Client } from '@stomp/stompjs';
import { apiClient } from '@/shared/api/client';
import useUser from '@/shared/hooks/useUser';
import { useAlert } from '@/shared/contexts/AlertContext';
import { getApiLang } from '@/shared/utils/lang';
import PageState from '@/shared/components/pageState/PageState';
import './ChatRoom.css';

interface Message {
  messageId: string;
  senderId: string;
  senderName: string;
  content: string;
  // 전송 시점에 백그라운드로 번역해서 채워지는 캐시. 아직 번역 전이거나 AI가
  // 끊겨 있었으면 비어 있고, 그때는 content(원문)를 그대로 보여준다.
  contentEn?: string;
  contentJa?: string;
  createdAt: string;
  // 이 메시지를 아직 안 읽은 참여자 수(카카오톡의 "1" 배지와 같은 의미). 0이 되면
  // 모두 읽은 것이라 배지를 숨긴다.
  unreadCount?: number;
}

// 누군가 방을 읽었을 때 서버가 전체 메시지를 다시 주는 대신 "이 메시지의 안읽은
// 사람 수가 이렇게 바뀌었다"만 가볍게 보내주는 델타 — 이 값으로 로컬 상태만 갱신하고
// 서버에 메시지 목록을 다시 요청하지 않는다.
interface UnreadDelta {
  messageId: number;
  unreadCount: number;
}

// 메시지 번역이 끝났을 때 서버가 별도 채널로 보내주는 패치 — 번역된 문구만 담겨 있어
// 이 값으로 기존 메시지의 contentEn/contentJa만 채워 넣는다.
interface TranslationDelta {
  messageId: string;
  contentEn: string;
  contentJa: string;
}

// 현재 화면 언어로 번역된 문구. 화면 언어가 한국어면 번역할 대상이 없으므로 undefined.
// 아직 번역이 끝나지 않았으면(전송 직후이거나 AI가 끊겨 있었으면)도 undefined.
const getTranslation = (message: Message): string | undefined => {
  const lang = getApiLang();
  if (lang === 'en') return message.contentEn || undefined;
  if (lang === 'ja') return message.contentJa || undefined;
  return undefined;
};

interface ChatRoomProps {
  roomId: string;
  title?: string;
  // 모임 관리 화면(모임장 전용)에 끼워 넣을 때는 나가기 자체가 불가능하므로 숨긴다.
  showLeave?: boolean;
}

export default function ChatRoom({ roomId, title, showLeave = true }: ChatRoomProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showAlert, showConfirm } = useAlert();
  const { data: user } = useUser();
  const [messages, setMessages] = useState<Message[]>([]);
  const [memberCount, setMemberCount] = useState(0);
  const [myState, setMyState] = useState<string | null>(null);
  const [content, setContent] = useState('');
  const [error, setError] = useState('');
  const [isLeaving, setIsLeaving] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  // 번역 버튼을 눌러 번역본을 보고 있는 메시지 id 집합. 기본값은 항상 원문이다.
  const [translatedIds, setTranslatedIds] = useState<Set<string>>(new Set());
  const messagesRef = useRef<HTMLDivElement>(null);

  const toggleTranslation = (messageId: string) => {
    setTranslatedIds((current) => {
      const next = new Set(current);
      if (next.has(messageId)) next.delete(messageId);
      else next.add(messageId);
      return next;
    });
  };

  const kicked = myState === 'K';

  // 방에 처음 들어오거나 새 메시지가 쌓일 때마다 가장 마지막 대화가 보이도록
  // 맨 아래로 스크롤한다. 이전 대화는 위로 스크롤해야 볼 수 있다.
  useEffect(() => {
    const el = messagesRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [messages]);

  // 전체 메시지(내용/발신자 등)를 다시 불러오는 건 방에 처음 들어올 때 한 번만 하고,
  // 그 뒤로는 "읽음" 자체만 가볍게 알리거나(markRead) 서버가 보내주는 안읽음 수
  // 델타만 반영해서 서버에 부담을 주지 않는다.
  const load = async () => {
    setIsLoading(true);
    try {
      const r = await apiClient.get<{ data: Message[]; memberCount: number; myState: string | null }>(`/chat/rooms/${roomId}/messages`);
      if (r.success) {
        setMessages(r.data.data);
        setMemberCount(r.data.memberCount ?? 0);
        setMyState(r.data.myState ?? null);
      } else {
        setError(t('chat.loadFailed'));
      }
    } finally {
      setIsLoading(false);
    }
  };

  const applyUnreadDelta = (delta: UnreadDelta[]) => {
    setMessages((current) => current.map((m) => {
      const match = delta.find((d) => String(d.messageId) === m.messageId);
      return match ? { ...m, unreadCount: match.unreadCount } : m;
    }));
  };

  const applyTranslationDelta = (delta: TranslationDelta) => {
    setMessages((current) => current.map((m) => (
      m.messageId === delta.messageId
        ? { ...m, contentEn: delta.contentEn || m.contentEn, contentJa: delta.contentJa || m.contentJa }
        : m
    )));
  };

  useEffect(() => {
    void load();
    const api = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    const wsUrl = api.replace(/^http/, 'ws') + '/ws/chat-native';
    const stomp = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 3000,
      onConnect: () => {
        stomp.subscribe(`/topic/chat/${roomId}`, (packet) => {
          setMessages((current) => [...current, JSON.parse(packet.body) as Message]);
          // 방을 열어둔 채로 새 메시지를 실시간으로 받는 것도 "읽은" 것이므로 읽음
          // 처리만 가볍게 알린다(전체 메시지 재조회 없음). 그 결과(델타)는 아래
          // /read 구독으로 돌아온다.
          void apiClient.put(`/chat/rooms/${roomId}/read`, {});
        });
        // 누군가 이 방을 읽으면(나 자신 포함) 바뀐 "안읽은 사람 수"만 가볍게 오는데,
        // 그걸로 로컬 메시지 상태만 갱신하고 서버에 다시 요청하지 않는다.
        stomp.subscribe(`/topic/chat/${roomId}/read`, (packet) => {
          const delta = JSON.parse(packet.body) as UnreadDelta[];
          applyUnreadDelta(delta);
        });
        // 전송 직후 백그라운드로 진행되는 번역이 끝나면, 해당 메시지의 문구만
        // 채워 넣는다(전체 메시지 재조회 없음).
        stomp.subscribe(`/topic/chat/${roomId}/translated`, (packet) => {
          applyTranslationDelta(JSON.parse(packet.body) as TranslationDelta);
        });
      },
    });
    stomp.activate();
    return () => { void stomp.deactivate(); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId]);

  const send = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!content.trim() || isSending) return;
    setIsSending(true);
    try {
      const r = await apiClient.post<unknown>(`/chat/rooms/${roomId}/messages`, { content, title });
      if (r.success) {
        setContent('');
        setError('');
      } else {
        setError(r.message || t('chat.sendFailed'));
      }
    } finally {
      setIsSending(false);
    }
  };

  const leaveRoom = () => {
    showConfirm(t('chat.leaveConfirm'), {
      confirmText: t('chat.leave'),
      onConfirm: async () => {
        setIsLeaving(true);
        const r = await apiClient.delete<unknown>(`/chat/rooms/${roomId}`);
        setIsLeaving(false);
        if (!r.success) {
          showAlert(r.message || t('chat.leaveFailed'));
          return;
        }
        queryClient.invalidateQueries({ queryKey: ['chatRooms'] });
        queryClient.invalidateQueries({ queryKey: ['myMoim'] });
        queryClient.invalidateQueries({ queryKey: ['chatUnreadCount'] });
        navigate('/chat', { replace: true });
      },
    });
  };

  return (
    <div className="chat-room">
      <div className="chat-room-meta">
        <span>{t('chat.memberCount', { count: memberCount })}</span>
        {showLeave && !kicked && (
          <button type="button" className="chat-leave-btn" onClick={leaveRoom} disabled={isLeaving}>
            {t('chat.leave')}
          </button>
        )}
      </div>
      {kicked && <p className="chat-kicked-banner">{t('chat.kickedBanner')}</p>}
      <div className="chat-messages" ref={messagesRef}>
        {isLoading ? (
          <PageState status="loading" fullScreen={false} />
        ) : messages.length === 0 ? (
          <PageState status="empty" message={t('chat.emptyMessages')} fullScreen={false} />
        ) : messages.map((m) => {
          const mine = m.senderId === user?.userId;
          const unread = m.unreadCount ?? 0;
          const translation = getTranslation(m);
          const wantsTranslation = translatedIds.has(m.messageId);
          const showingTranslation = wantsTranslation && !!translation;
          const canTranslate = getApiLang() !== 'ko';
          return (
            <article key={m.messageId} className={mine ? 'mine' : ''}>
              <b>{mine ? t('chat.me') : m.senderName}</b>
              <span className="chat-bubble-row">
                {mine && unread > 0 && <em className="chat-unread-badge">{unread}</em>}
                <span className="chat-bubble-content">
                  <p>{showingTranslation ? translation : m.content}</p>
                  {canTranslate && (
                    <button
                      type="button"
                      className="chat-translate-btn"
                      onClick={() => toggleTranslation(m.messageId)}
                      disabled={wantsTranslation && !translation}
                    >
                      {showingTranslation
                        ? t('chat.showOriginal')
                        : wantsTranslation
                          ? t('chat.translating')
                          : t('chat.translate')}
                    </button>
                  )}
                </span>
                {!mine && unread > 0 && <em className="chat-unread-badge">{unread}</em>}
              </span>
            </article>
          );
        })}
      </div>
      {error && <p className="chat-room-error" role="alert">{error}</p>}
      <form onSubmit={send}>
        <input
          value={content}
          onChange={(e) => setContent(e.target.value)}
          maxLength={1000}
          placeholder={t('chat.placeholder')}
          aria-label={t('chat.placeholder')}
          disabled={kicked}
        />
        <button type="submit" className="chat-send-btn" aria-label={t('chat.send')} disabled={kicked || isSending}>➤</button>
      </form>
    </div>
  );
}
