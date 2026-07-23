import MainLayout from "@/layouts/MainLayout"
import { BrowserRouter, Route, Routes } from "react-router-dom"
import HomePage  from '@/features/home/pages/HomePage';
import GuidePage from "@/features/guide/pages/GuidePage";

const Router = () => {
    return (
      <BrowserRouter>
        <Routes>
          <Route>
            <Route path="/guide" element={<GuidePage />} />
          </Route>
          <Route element={<MainLayout />}>
            <Route path="/" element={<HomePage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    )
}
export default Router