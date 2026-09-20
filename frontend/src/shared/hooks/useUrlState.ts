import { useSearchParams } from "react-router-dom";

// 필터/탭처럼 "새로고침해도 유지됐으면 하는" 화면 상태를 URL 쿼리스트링에 저장한다.
// useState만 쓰면 새로고침할 때마다 기본값으로 초기화되는 문제가 있어서, 값을 URL에
// 실어두고 그걸 그대로 읽어 초기 상태로 쓴다. 값이 기본값이면 쿼리스트링에서 지워서
// URL이 지저분해지는 걸 막고, 히스토리에 탭 전환마다 항목이 쌓이지 않도록 replace로 바꾼다.
export const useUrlState = (paramName: string, defaultValue: string) => {
  const [searchParams, setSearchParams] = useSearchParams();
  const value = searchParams.get(paramName) ?? defaultValue;

  const setValue = (next: string) => {
    setSearchParams(
      (prev) => {
        const nextParams = new URLSearchParams(prev);
        if (next === defaultValue) {
          nextParams.delete(paramName);
        } else {
          nextParams.set(paramName, next);
        }
        return nextParams;
      },
      { replace: true }
    );
  };

  return [value, setValue] as const;
};

export default useUrlState;
