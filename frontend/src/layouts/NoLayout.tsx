import { Outlet } from 'react-router-dom'
import useScrollToTop from '@/shared/hooks/useScrollToTop'

// 공통 헤더/네비게이션이 없는 화면(로그인, 채팅방, 소모임 생성 등)도 다른
// 레이아웃과 동일하게 페이지 이동 시 스크롤을 맨 위로 되돌리기 위한 래퍼.
const NoLayout = () => {
  useScrollToTop();
  return <Outlet />
}

export default NoLayout
