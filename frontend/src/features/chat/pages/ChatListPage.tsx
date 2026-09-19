import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { apiClient } from '@/shared/api/client';
import useUser from '@/shared/hooks/useUser';
import FilterTabs from '@/shared/components/filterTabs/FilterTabs';
import PageState from '@/shared/components/pageState/PageState';
import './ChatListPage.css';

interface Room {
  roomId: string;
  title: string;
  lastMessage: string | null;
  unreadCount: number;
}

type RoomFilter = 'all' | 'unread';

export default function ChatListPage() {
  const { t } = useTranslation();
  const [filter, setFilter] = useState<RoomFilter>('all');
  const { data: user, isLoading: userLoading } = useUser();
  const isLoggedIn = !!user;

  const rooms = useQuery({
    queryKey: ['chatRooms'],
    queryFn: async () => {
      const r = await apiClient.get<{ data: Room[] }>('/chat/rooms');
      if (!r.success) throw r;
      return r.data.data;
    },
    retry: 1,
    enabled: isLoggedIn,
  });

  const visibleRooms = (rooms.data ?? []).filter((room) => filter === 'all' || room.unreadCount > 0);

  if (userLoading) return <PageState status="loading" />;

  if (!isLoggedIn) {
    return (
      <div className="chat-list-page">
        <section className="chat-guest">
          <div className="chat-guest-avatar" aria-hidden="true">💬</div>
          <p className="chat-guest-desc">{t('chat.loginRequired')}</p>
          <Link to="/auth" className="chat-guest-login">{t('account.login')}</Link>
        </section>
      </div>
    );
  }

  return (
    <div className="chat-list-page">
      <p className="chat-list-subtitle">{t('chat.subtitle')}</p>

      <FilterTabs
        options={[
          { id: 'all', label: t('chat.filterAll') },
          { id: 'unread', label: t('chat.filterUnread') },
        ]}
        activeId={filter}
        onChange={(id) => setFilter(id as RoomFilter)}
      />

      {rooms.isLoading ? (
        <PageState status="loading" message={t('chat.loadingRooms')} />
      ) : rooms.isError ? (
        <PageState status="error" onRetry={() => rooms.refetch()} />
      ) : visibleRooms.length === 0 ? (
        filter === 'unread' ? (
          <p className="chat-state">{t('chat.emptyUnread')}</p>
        ) : (
          <div className="chat-state">
            <p>{t('chat.emptyRooms')}</p>
            <p className="chat-state-cta">{t('chat.emptyRoomsCta')}</p>
          </div>
        )
      ) : (
        <ul>
          {visibleRooms.map((room) => (
            <li key={room.roomId}>
              <Link to={`/chat/${room.roomId}`}>
                <div className="room-avatar">💬</div>
                <div>
                  <b>{room.title}</b>
                  <p>{room.lastMessage}</p>
                </div>
                {room.unreadCount > 0 && (
                  <em className="room-unread-badge">{room.unreadCount > 99 ? '99+' : room.unreadCount}</em>
                )}
                <i>›</i>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
