import './AiWaitPopup.css';

interface AiWaitPopupProps {
  message: string;
}

// AI 응답을 기다리는 동안 화면 하단에 뜨는 비침습적 안내. AlertModal과 달리
// 화면을 가리거나 조작을 막지 않고, 스크린리더에는 role="status"로 즉시 안내된다.
const AiWaitPopup = ({ message }: AiWaitPopupProps) => {
  return (
    <div className="ai-wait-popup" role="status" aria-live="polite">
      <span className="ai-wait-popup-spinner" aria-hidden="true" />
      <p className="ai-wait-popup-message">{message}</p>
    </div>
  );
};

export default AiWaitPopup;
