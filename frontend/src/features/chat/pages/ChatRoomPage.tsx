import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import ChatRoom from '../components/ChatRoom';
import './ChatRoomPage.css';

export default function ChatRoomPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { roomId = 'lobby' } = useParams();
  const location = useLocation();
  const roomTitle = (location.state as { title?: string } | null)?.title;

  return (
    <main className="chat-page">
      <header className="chat-room-header">
        <button type="button" onClick={() => navigate(-1)} aria-label={t('chat.backToList')}>‹</button>
        <div>
          <b>{roomTitle ?? t('chat.roomTitle')}</b>
          <span>{t('chat.roomSubtitle')}</span>
        </div>
      </header>
      <ChatRoom roomId={roomId} title={roomTitle} />
    </main>
  );
}
