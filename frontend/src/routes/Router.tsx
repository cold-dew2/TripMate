import SignupPage from "@/features/auth/pages/SignupPage"
import { BrowserRouter, Route, Routes } from "react-router-dom"

const Router = () => {
    return (
      <BrowserRouter>
        <Routes>
          <Route path="/signup" element={<SignupPage/>} />
        </Routes>
      </BrowserRouter>
    )
}
export default Router