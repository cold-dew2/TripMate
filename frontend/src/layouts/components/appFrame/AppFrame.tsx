import type { ReactNode } from "react";
import "./AppFrame.css";

const AppFrame = ({ children }: { children: ReactNode }) => {
  return (
    <div className="app-shell">
      <div className="app-frame">{children}</div>
    </div>
  );
};

export default AppFrame;
