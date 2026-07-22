import React from 'react'
import HomeHeader from './components/header/homeHeader/HomeHeader'
import { Outlet } from 'react-router-dom'
import Navigation from './components/nav/Navigation'

const MainLayout = () => {
  return (
    <>
      <HomeHeader />
      <main>
        <Outlet />
      </main>
      <Navigation />
    </>
  )
}

export default MainLayout