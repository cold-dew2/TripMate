import { Outlet, useMatches } from 'react-router-dom';
import type { RouteHandle } from '@/routes/path/paths';
import { useTranslation } from 'react-i18next';
import PageHeader from './components/header/pageHeader/PageHeader'
import Navigation from './components/nav/Navigation';

const ContentLayout = () => {
  const { t } = useTranslation();
  const matches = useMatches();
  const current = [...matches].reverse().find((m) => (m.handle as RouteHandle)?.title);
  const handle = current?.handle as RouteHandle | undefined;
  const title = handle?.title ?? '';
  const showBack = handle?.showBack ?? true;
  const href = handle?.href ?? '';
  const linkText = handle?.linkText ?? '';
  const currentPage = handle?.current ?? undefined;
  const totalPage = handle?.total ?? undefined;


  return (
    <>
      <PageHeader 
        pageTitle={!showBack ? t(title) : undefined} 
        contentTitle={showBack ? t(title) : undefined}
        href={href}
        linkText={t(linkText)}
        current={currentPage}
        total={totalPage}
      />
      
      <main className="container">
        <Outlet/>
      </main>

      <Navigation />
    </>
  )
}

export default ContentLayout