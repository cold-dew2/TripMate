import React from 'react'
import PageHeader from './components/header/pageHeader/PageHeader'
import { Outlet } from 'react-router-dom';
import Navigation from './components/nav/Navigation';

interface LayoutProps {
  title: string;
}

const DetailLayout = ({title}: LayoutProps) => {
  return (
    <>
      <PageHeader title={title} />
      
      <main>
        <Outlet/>
      </main>

      <Navigation />
    </>
  )
}

export default DetailLayout