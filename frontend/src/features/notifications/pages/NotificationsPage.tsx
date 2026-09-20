import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import useNotifications, { useMarkNotificationRead, type Notification } from '../hooks/useNotifications';
import PageState from '@/shared/components/pageState/PageState';
import './NotificationsPage.css';

const ICON_BY_TYPE: Record<Notification['typeCd'], string> = {
  APPLY: '📝',
  CHAT: '💬',
  COMPLETE: '🎉',
};

// 알림 문구는 사용자/모임 이름이 매번 달라 고정 번역사전(t(원문))으로 처리할 수 없어,
// 타입별로 필요한 값(param1/param2)만 저장해두고 여기서 화면 언어에 맞는 문장으로
// 조립한다. CHAT은 실제 채팅 메시지 원문이라 번역까지는 지원하지 않고 그대로 보여준다.
const renderNotificationText = (notification: Notification, t: (key: string, opts?: Record<string, unknown>) => string) => {
  switch (notification.typeCd) {
    case 'APPLY':
      return {
        title: t('notifications.applyTitle'),
        content: t('notifications.applyContent', { userName: notification.param1, moimTitle: notification.param2 }),
      };
    case 'COMPLETE':
      return {
        title: t('notifications.completeTitle'),
        content: t('notifications.completeContent', { moimTitle: notification.param1 }),
      };
    case 'CHAT':
    default:
      return { title: notification.title, content: notification.content };
  }
};

const NotificationsPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { data: notifications, isLoading, isError, error } = useNotifications();
  const markRead = useMarkNotificationRead();
  const needsLogin = (error as { code?: string } | null)?.code === 'NEED_LOGIN';

  const handleClick = (notification: Notification) => {
    if (notification.isRead === 'N') markRead.mutate(notification.notiId);
    if (notification.linkUrl) navigate(notification.linkUrl);
  };

  return (
    <div className="notifications-page">
      {isLoading ? (
        <PageState status="loading" />
      ) : needsLogin ? (
        <PageState status="empty" message={t('notifications.loginRequired')} />
      ) : isError ? (
        <PageState status="error" />
      ) : !notifications?.length ? (
        <PageState status="empty" message={t('notifications.empty')} />
      ) : (
        <ul className="notifications-list">
          {notifications.map((notification) => {
            const text = renderNotificationText(notification, t);
            return (
              <li key={notification.notiId}>
                <button
                  type="button"
                  className={notification.isRead === 'N' ? 'notification-row unread' : 'notification-row'}
                  onClick={() => handleClick(notification)}
                >
                  <span className="notification-icon" aria-hidden="true">{ICON_BY_TYPE[notification.typeCd]}</span>
                  <span className="notification-body">
                    <strong>{text.title}</strong>
                    <p>{text.content}</p>
                    <time>{notification.createDt}</time>
                  </span>
                  {notification.isRead === 'N' && <span className="notification-dot" aria-hidden="true" />}
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
};

export default NotificationsPage;
