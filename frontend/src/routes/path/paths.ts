import type { RouteObject } from "react-router-dom";

export interface RouteHandle {
  title: string;
  showBack?: boolean;
  href?: string;
  linkText?: string;
  current?: number;
  total?: number;
}


// GuideLayout 쓰는 페이지들
export const guideRoutes: (RouteObject & { handle?: RouteHandle })[] = [
  {
    path: "/guide",
    lazy: () => import("@/features/guide/pages/GuidePage").then((m) => ({ Component: m.default })),
  },
];

// MainLayout 쓰는 페이지들
export const mainRoutes: (RouteObject & { handle?: RouteHandle })[] = [
  {
    path: "/",
    lazy: () => import("@/features/home/pages/HomePage").then((m) => ({ Component: m.default })),
  },
];

// ContentLayout 쓰는 페이지들
export const contentRoutes: (RouteObject & { handle?: RouteHandle })[] = [
  {
    path: "/moimList",
    lazy: () => import("@/features/moim/pages/moimList/MoimList").then((m) => ({ Component: m.default })),
    handle: { title: "moim.listTitle", showBack: false, href: "/", linkText: "moim.myPlan"},
  },
  {
    path: "/placeList",
    lazy: () => import("@/features/place/pages/placeList/PlaceList").then((m) => ({ Component: m.default })),
    handle: { title: "place.listTitle", showBack: false},
  },
  {
    path: "/moim/:moimId",  
    lazy: () => import("@/features/moim/pages/moimDetail/MoimDetail").then((m) => ({ Component: m.default })),
    handle: { showBack: true },
  },
  {
    path: "/place/:tourId", 
    lazy: () => import("@/features/place/pages/placeDetail/PlaceDetail").then((m) => ({ Component: m.default })),
    handle: { showBack: true },
  },
];