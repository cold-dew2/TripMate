import MainLayout from "@/layouts/MainLayout"
import { BrowserRouter, Route, Routes } from "react-router-dom"
import HomePage  from '@/features/home/pages/HomePage';

const Router = () => {
    return (
      <BrowserRouter>
        <Routes>
          <Route element={<MainLayout />}>
            <Route path="/" element={<HomePage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    )
}
export default Router