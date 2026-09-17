import MainLayout from "@/layouts/MainLayout"
import { createBrowserRouter, RouterProvider } from "react-router-dom"
import { contentRoutes, mainRoutes, myRoutes, noLayoutRoutes } from "./path/paths";
import ContentLayout from "@/layouts/ContentLayout";
import MyLayout from "@/layouts/MyLayout";

const router = createBrowserRouter([
  {
    element: <MainLayout />,
    children: mainRoutes,
  },
  {
    element: <ContentLayout />,
    children: contentRoutes,
  },
  {
    element: <MyLayout />,
    children: myRoutes,
  },
  ...noLayoutRoutes,
]);
const Router = () => <RouterProvider router={router} />;
export default Router