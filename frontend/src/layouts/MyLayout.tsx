import { Outlet } from 'react-router-dom'
import Navigation from './components/nav/Navigation'

const MyLayout = () => {
  return (
    <>
      <Outlet />
      <Navigation />
    </>
  )
}

export default MyLayout
