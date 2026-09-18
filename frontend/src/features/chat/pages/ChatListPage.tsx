import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { apiClient } from '@/shared/api/client';
import FilterTabs from '@/shared/components/filterTabs/FilterTabs';
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
  const rooms = useQuery({
    queryKey: ['chatRooms'],
    queryFn: async () => {
      const r = await apiClient.get<{ data: Room[] }>('/chat/rooms');
      if (!r.success) throw r;
      return r.data.data;
    },
    retry: 1,
  });

  const visibleRooms = (rooms.data ?? []).filter((room) => filter === 'all' || room.unreadCount > 0);

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
        <p className="chat-state">{t('chat.loadingRooms')}</p>
      ) : rooms.isError ? (
        <p className="chat-state">{t('common.loadError')}</p>
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
