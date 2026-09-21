import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Button from '@/shared/components/button/Button';
import './NotFoundPage.css';

const NotFoundPage = () => {
  const { t } = useTranslation();

  return (
    <main className="not-found-page">
      <p className="not-found-code">404</p>
      <p className="not-found-title">{t('notFound.title')}</p>
      <p className="not-found-desc">{t('notFound.desc')}</p>
      <Button as={Link} to="/" text={t('notFound.goHome')} />
    </main>
  );
};

export default NotFoundPage;
