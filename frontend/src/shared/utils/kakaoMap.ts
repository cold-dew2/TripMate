const KAKAO_MAP_KEY = import.meta.env.VITE_KAKAO_MAP_KEY as string | undefined;

let loadingPromise: Promise<typeof kakao> | null = null;

// 카카오맵 JS SDK는 <script> 태그로 한 번만 로드해서 재사용해야 하므로
// 앱 전체에서 공유하는 싱글턴 프로미스로 관리한다.
export const loadKakaoMap = (): Promise<typeof kakao> => {
  if (window.kakao?.maps) return Promise.resolve(window.kakao);
  if (loadingPromise) return loadingPromise;

  if (!KAKAO_MAP_KEY) {
    return Promise.reject(new Error("VITE_KAKAO_MAP_KEY가 설정되지 않았습니다."));
  }

  loadingPromise = new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_MAP_KEY}&autoload=false&libraries=services`;
    script.async = true;
    script.onload = () => {
      window.kakao.maps.load(() => resolve(window.kakao));
    };
    script.onerror = () => {
      loadingPromise = null;
      reject(new Error("카카오맵 SDK를 불러오지 못했습니다."));
    };
    document.head.appendChild(script);
  });

  return loadingPromise;
};
