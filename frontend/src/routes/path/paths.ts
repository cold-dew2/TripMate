import type { RouteObject } from "react-router-dom";

export interface RouteHandle {
  title?: string;
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
    handle: { title: "moim.listTitle", showBack: true, href: "/my/schedule", linkText: "moim.myPlan"},
  },
  {
    path: "/moimList/sejong",
    lazy: () => import("@/features/moim/pages/sejongMate/SejongMatePage").then((m) => ({ Component: m.default })),
    handle: { title: "sejongMate.title", showBack: true },
  },
  {
    path: "/placeList",
    lazy: () => import("@/features/place/pages/placeList/PlaceList").then((m) => ({ Component: m.default })),
    handle: { title: "place.listTitle", showBack: true },
  },
  {
    path: "/place/:tourId",
    lazy: () => import("@/features/place/pages/placeDetail/PlaceDetail").then((m) => ({ Component: m.default })),
    handle: { title: "place.details", showBack: true },
  },
  {
    path: "/moim/:moimId",
    lazy: () => import("@/features/moim/pages/moimDetail/MoimDetail").then((m) => ({ Component: m.default })),
    handle: { title: "moim.detailsTitle", showBack: true },
  },
  {
    path: "/moimManage",
    lazy: () => import("@/features/moim/pages/moimManage/MoimManage").then((m) => ({ Component: m.default })),
    handle: { title: "moim.manageTitle", showBack: true },
  },
  {
    path: "/moimManage/:moimId",
    lazy: () => import("@/features/moim/pages/moimManage/MoimManageDetail").then((m) => ({ Component: m.default })),
    handle: { title: "moim.manageTitle", showBack: true },
  },
  {
    path: "/safetyReport",
    lazy: () => import("@/features/my/pages/safetyReport/SafetyReportScreen").then((m) => ({ Component: m.default })),
    handle: { title: "safetyReport.title", showBack: true },
  },
  {
    path: "/chat",
    lazy: () => import("@/features/chat/pages/ChatListPage").then((m) => ({ Component: m.default })),
    handle: { title: "nav.chat", showBack: true },
  },
  {
    path: "/my/schedule",
    lazy: () => import("@/features/my/pages/mySchedule/MySchedule").then((m) => ({ Component: m.default })),
    handle: { title: "my.myPlanTitle", showBack: true },
  },
  {
    path: "/search",
    lazy: () => import("@/features/search/pages/SearchResultPage").then((m) => ({ Component: m.default })),
    handle: { title: "search.title", showBack: true },
  },
  {
    path: "/my/edit",
    lazy: () => import("@/features/my/pages/profileEdit/ProfileEdit").then((m) => ({ Component: m.default })),
    handle: { title: "account.edit", showBack: true },
  },
  {
    path: "/my/reviews",
    lazy: () => import("@/features/my/pages/myReviews/MyReviewsPage").then((m) => ({ Component: m.default })),
    handle: { title: "my.recentlyReviews", showBack: true },
  },
  {
    path: "/notifications",
    lazy: () => import("@/features/notifications/pages/NotificationsPage").then((m) => ({ Component: m.default })),
    handle: { title: "notifications.title", showBack: true },
  },
  {
    path: "/users/:userId",
    lazy: () => import("@/features/users/pages/UserProfilePage").then((m) => ({ Component: m.default })),
    handle: { title: "userProfile.title", showBack: true },
  },
];

// MyLayout(헤더 없이 하단 네비게이션만) 쓰는 페이지
export const myRoutes: (RouteObject & { handle?: RouteHandle })[] = [
  {
    path: "/my",
    lazy: () => import("@/features/my/pages/myPage/MyPageScreen").then((m) => ({ Component: m.default })),
  },
];

export const noLayoutRoutes: (RouteObject & { handle?: RouteHandle })[] = [
  {
    path: "/auth",
    lazy: () => import("@/features/auth/pages/LoginPage").then((m) => ({ Component: m.default })),
  },
  {
    path: "/auth/signup",
    lazy: () => import("@/features/auth/pages/SignupPage").then((m) => ({ Component: m.default })),
  },
  {
    path: "/auth/find-id",
    lazy: () => import("@/features/auth/pages/FindIdPage").then((m) => ({ Component: m.default })),
  },
  {
    path: "/auth/find-password",
    lazy: () => import("@/features/auth/pages/FindPasswordPage").then((m) => ({ Component: m.default })),
  },
  {
    path: "/chat/:roomId",
    lazy: () => import("@/features/chat/pages/ChatRoomPage").then((m) => ({ Component: m.default })),
  },
  {
    path: "/createMoim",
    lazy: () => import("@/features/moim/pages/moimCreate/MoimCreate").then((m) => ({ Component: m.default })),
  },
  {
    path: "/moim/:moimId/review",
    lazy: () => import("@/features/moim/pages/moimReview/MoimReviewPage").then((m) => ({ Component: m.default })),
  },
];
