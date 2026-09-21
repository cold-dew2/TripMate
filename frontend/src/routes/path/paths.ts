import { redirect, type RouteObject } from "react-router-dom";
import { apiClient } from "@/shared/api/client";

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
    // 소모임 생성은 로그인한 사용자만 가능하다. URL로 직접 들어오는 것도 막기
    // 위해 라우터 loader 단계에서 로그인 여부를 확인해, 게스트면 화면이 그려지기
    // 전에 로그인 화면으로 돌려보낸다.
    loader: async () => {
      const result = await apiClient.get<unknown>("/login/authCheck");
      if (!result.success) {
        throw redirect("/auth");
      }
      return null;
    },
    lazy: () => import("@/features/moim/pages/moimCreate/MoimCreate").then((m) => ({ Component: m.default })),
  },
  {
    path: "/moim/:moimId/review",
    lazy: () => import("@/features/moim/pages/moimReview/MoimReviewPage").then((m) => ({ Component: m.default })),
  },
  // 정의된 라우트와 매칭되지 않는 모든 경로. 반드시 배열 마지막에 있어야 한다
  // (react-router는 등록 순서와 무관하게 가장 구체적인 경로부터 매칭하지만,
  // "*"는 다른 모든 경로와 매칭되므로 의도를 명확히 하기 위해 마지막에 둔다).
  {
    path: "*",
    lazy: () => import("@/features/notFound/pages/NotFoundPage").then((m) => ({ Component: m.default })),
  },
];
