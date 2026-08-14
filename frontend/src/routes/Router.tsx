import MainLayout from "@/layouts/MainLayout"
import { createBrowserRouter, RouterProvider } from "react-router-dom"
import { contentRoutes, mainRoutes, noLayoutRoutes } from "./path/paths";
import ContentLayout from "@/layouts/ContentLayout";

const router = createBrowserRouter([
  {
    element: <MainLayout />,
    children: mainRoutes,
  },
  {
    element: <ContentLayout />,
    children: contentRoutes,
  },
  ...noLayoutRoutes,
]); 
const Router = () => <RouterProvider router={router} />;
export default Router