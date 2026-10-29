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
  // 누군가 번역을 요청했을 때 서버에 저장되는 언어별 번역. 없으면 번역 버튼을 누를 때 요청한다.
  contentEn?: string;
  contentJa?: string;
  contentKo?: string;
  // 원문 언어(ko/en/ja). 화면 언어와 같으면 번역 버튼을 보여주지 않는다.
  srcLangCd?: string | null;
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

// 메시지 한 건 번역 요청의 결과. FAILED면 AI가 실패한 것이라 원문을 그대로 둔다.
interface MessageTranslation {
  messageId: string;
  lang: string;
  content: string;
  state: 'SOURCE' | 'CACHED' | 'TRANSLATED' | 'FAILED';
}

const translationKey = { ko: 'contentKo', en: 'contentEn', ja: 'contentJa' } as const;

// 현재 화면 언어로 저장된 번역. 아직 아무도 요청하지 않았으면 undefined.
const getTranslation = (message: Message): string | undefined => {
  const lang = getApiLang() as keyof typeof translationKey;
  const key = translationKey[lang];
  return key ? message[key] || undefined : undefined;
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
  const [isConnected, setIsConnected] = useState(true);
  // 번역 버튼을 눌러 번역본을 보고 있는 메시지 id 집합. 기본값은 항상 원문이다.
  const [translatedIds, setTranslatedIds] = useState<Set<string>>(new Set());
  const messagesRef = useRef<HTMLDivElement>(null);

  const setTranslated = (messageId: string, on: boolean) => {
    setTranslatedIds((current) => {
      const next = new Set(current);
      if (on) next.add(messageId);
      else next.delete(messageId);
      return next;
    });
  };

  // 번역이 저장돼 있으면 바로 전환하고, 없을 때만 서버에 요청한다(서버가 메시지·언어별로 저장해
  // 같은 메시지를 다시 번역하지 않음). 실패하면 원문으로 되돌리고 알린다.
  const toggleTranslation = async (message: Message) => {
    const { messageId } = message;
    if (translatedIds.has(messageId)) {
      setTranslated(messageId, false);
      return;
    }
    setTranslated(messageId, true);
    if (getTranslation(message)) return;
    const lang = getApiLang() as keyof typeof translationKey;
    const r = await apiClient.post<{ data: MessageTranslation }>(
      `/chat/rooms/${roomId}/messages/${messageId}/translate?lang=${lang}`,
      {},
    );
    const result = r.success ? r.data?.data : undefined;
    if (!result || result.state === 'FAILED' || !translationKey[lang]) {
      setTranslated(messageId, false);
      showAlert(t('chat.translateFailed'));
      return;
    }
    setMessages((current) => current.map((m) => (
      m.messageId === messageId ? { ...m, [translationKey[lang]]: result.content } : m
    )));
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
  const load = async (opts: { silent?: boolean } = {}) => {
    if (!opts.silent) setIsLoading(true);
    try {
      const r = await apiClient.get<{ data: Message[]; memberCount: number; myState: string | null }>(`/chat/rooms/${roomId}/messages`);
      if (r.success) {
        setMessages(r.data.data);
        setMemberCount(r.data.memberCount ?? 0);
        setMyState(r.data.myState ?? null);
      } else if (!opts.silent) {
        setError(t('chat.loadFailed'));
      }
    } finally {
      if (!opts.silent) setIsLoading(false);
    }
  };

  const applyUnreadDelta = (delta: UnreadDelta[]) => {
    setMessages((current) => current.map((m) => {
      const match = delta.find((d) => String(d.messageId) === m.messageId);
      return match ? { ...m, unreadCount: match.unreadCount } : m;
    }));
  };

  useEffect(() => {
    void load();
    // 연결이 끊겼다가 자동 재연결되면(네트워크 순단 등) onConnect가 다시 불리는데,
    // 그 사이 다른 사람이 보낸 메시지는 실시간 구독이 끊겨 있어 아예 못 받는다.
    // 재연결 시점(최초 연결 제외)에 조용히(스피너 없이) 목록을 다시 불러와 메운다.
    let hasConnectedBefore = false;
    const api = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    const wsUrl = api.replace(/^http/, 'ws') + '/ws/chat-native';
    const stomp = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 3000,
      onConnect: () => {
        setIsConnected(true);
        if (hasConnectedBefore) {
          void load({ silent: true });
        }
        hasConnectedBefore = true;
        stomp.subscribe(`/topic/chat/${roomId}`, (packet) => {
          const incoming = JSON.parse(packet.body) as Message;
          // 내가 보낸 메시지는 REST 응답을 받은 시점에 이미 화면에 반영해두므로(WS가
          // 끊겨 있어도 메시지가 사라지지 않도록), 여기 에코가 나중에 와도 같은
          // messageId면 중복 추가하지 않는다.
          setMessages((current) => (
            current.some((m) => m.messageId === incoming.messageId) ? current : [...current, incoming]
          ));
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
      },
      // 연결이 끊기면(네트워크 순단, 서버 재시작 등) 사용자가 그걸 알 방법이 전혀
      // 없었다 — 실시간 수신이 멈춘 채로 계속 채팅 중인 것처럼 보였다.
      onDisconnect: () => setIsConnected(false),
      onWebSocketClose: () => setIsConnected(false),
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
      const r = await apiClient.post<{ data: Message }>(`/chat/rooms/${roomId}/messages`, { content, title });
      if (r.success) {
        // WebSocket이 끊겨 있으면 전송은 REST로 성공하고 입력창도 비워지는데 정작
        // 메시지는 화면에 안 뜨는 문제가 있었다(표시가 전적으로 WS 에코에만 의존했음).
        // 응답에 담긴 메시지를 바로 반영한다 — 나중에 WS 에코가 와도 위에서 messageId로
        // 중복을 막는다.
        if (r.data?.data) {
          const sent = r.data.data;
          setMessages((current) => (
            current.some((m) => m.messageId === sent.messageId) ? current : [...current, sent]
          ));
        }
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
          const canTranslate = !!m.srcLangCd && m.srcLangCd !== getApiLang();
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
                      onClick={() => { void toggleTranslation(m); }}
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
      {!isConnected && <p className="chat-room-error" role="status">{t('chat.reconnecting')}</p>}
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
