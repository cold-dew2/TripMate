// 백엔드가 관광지/소모임 카테고리를 GROUP_CONCAT으로 콤마 구분된 문자열로 내려준다
// (예: "가족여행,강·호수,공원"). MoimCard/SpotCard의 badge prop은 이 문자열을 그대로
// 받아 내부에서 다시 콤마로 쪼개 뱃지 여러 개로 렌더링하므로, t()를 문자열 전체에
// 걸면(각 카테고리 단어가 번역사전의 키라서) 절대 매칭되지 않는다. 항목별로 나눠
// 번역한 뒤 다시 콤마로 합쳐서 넘겨야 한다.
export const translateCategoryList = (value: string | undefined | null, t: (key: string) => string): string => {
  if (!value) return "";
  return value
    .split(",")
    .map((item) => t(item.trim()))
    .join(",");
};
