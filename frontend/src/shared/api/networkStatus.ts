// fetch 자체가 실패(네트워크 단절, 백엔드 서버 다운 등)했을 때를 감지하기 위한
// 아주 얇은 이벤트 채널. apiClient는 React 컴포넌트가 아니라 훅(useAlert 등)을 직접
// 쓸 수 없으므로, 여기 등록해둔 리스너를 통해 앱 루트 쪽(AlertProvider)에 알린다.
type Listener = () => void;

let listeners: Listener[] = [];

export const onBackendUnreachable = (listener: Listener) => {
  listeners.push(listener);
  return () => {
    listeners = listeners.filter((l) => l !== listener);
  };
};

export const notifyBackendUnreachable = () => {
  listeners.forEach((listener) => listener());
};
