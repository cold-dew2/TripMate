import { useEffect } from 'react';
import { useLocation, useNavigationType } from 'react-router-dom';

// 페이지 이동은 물론 뒤로가기/앞으로가기(popstate)에서도 항상 화면 맨 위부터
// 보이게 한다. react-router의 기본 ScrollRestoration은 뒤로가기 시 이전 스크롤
// 위치를 복원하는데, 이 앱에서는 매번 첫 화면부터 보이는 쪽을 원해 직접 처리한다.
const useScrollToTop = () => {
  const { pathname } = useLocation();
  const navigationType = useNavigationType();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname, navigationType]);
};

export default useScrollToTop;
