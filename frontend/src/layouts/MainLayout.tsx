import HomeHeader from './components/header/homeHeader/HomeHeader'
import { Outlet } from 'react-router-dom'
import Navigation from './components/nav/Navigation'
import useScrollToTop from '@/shared/hooks/useScrollToTop'

const MainLayout = () => {
  useScrollToTop();
  return (
    <>
      <HomeHeader />
      <main className="container">
        <Outlet />
      </main>
      <Navigation />
    </>
  )
}

export default MainLayout