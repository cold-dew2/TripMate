import { Outlet } from 'react-router-dom'
import Navigation from './components/nav/Navigation'
import useScrollToTop from '@/shared/hooks/useScrollToTop'

const MyLayout = () => {
  useScrollToTop();
  return (
    <>
      <Outlet />
      <Navigation />
    </>
  )
}

export default MyLayout
