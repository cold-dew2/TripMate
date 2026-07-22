import { Outlet } from 'react-router-dom'
import Navigation from './components/nav/Navigation'

const SearchLayout = () => {
  return (
    <>
      <main>
        <Outlet />
      </main>

      <Navigation />
    </>
  )
}

export default SearchLayout