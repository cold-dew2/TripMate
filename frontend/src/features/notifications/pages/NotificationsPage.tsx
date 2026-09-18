import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import useNotifications, { useMarkNotificationRead, type Notification } from '../hooks/useNotifications';
import './NotificationsPage.css';

const ICON_BY_TYPE: Record<Notification['typeCd'], string> = {
  APPLY: '📝',
  CHAT: '💬',
  COMPLETE: '🎉',
};

const NotificationsPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { data: notifications, isLoading, isError } = useNotifications();
  const markRead = useMarkNotificationRead();

  const handleClick = (notification: Notification) => {
    if (notification.isRead === 'N') markRead.mutate(notification.notiId);
    if (notification.linkUrl) navigate(notification.linkUrl);
  };

  return (
    <div className="notifications-page">
      {isLoading ? (
        <p className="notifications-state">{t('account.loading')}</p>
      ) : isError ? (
        <p className="notifications-state">{t('common.loadError')}</p>
      ) : !notifications?.length ? (
        <p className="notifications-state">{t('notifications.empty')}</p>
      ) : (
        <ul className="notifications-list">
          {notifications.map((notification) => (
            <li key={notification.notiId}>
              <button
                type="button"
                className={notification.isRead === 'N' ? 'notification-row unread' : 'notification-row'}
                onClick={() => handleClick(notification)}
              >
                <span className="notification-icon" aria-hidden="true">{ICON_BY_TYPE[notification.typeCd]}</span>
                <span className="notification-body">
                  <strong>{notification.title}</strong>
                  <p>{notification.content}</p>
                  <time>{notification.createDt}</time>
                </span>
                {notification.isRead === 'N' && <span className="notification-dot" aria-hidden="true" />}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default NotificationsPage;
