import { useState } from "react";
import './Tab.css'

interface Tab {
  id: string,
  label: string
}

interface TabProps {
  tabs: Tab[];
  type?: "default" | "scroll"
}

const Tab = ({ tabs, type = "default" }: TabProps) => {
  const [activeTab, setActiveTab] = useState(tabs[0]?.id);

  const handleTabClick = (id: string) => {
    setActiveTab(id);

    if (type === "scroll") {
      const target = document.getElementById(id);

      if (!target) return;

      target.scrollIntoView({
        behavior: "smooth",
        block: "start"
      });

      target.focus({ preventScroll: true });
    }
    
  }

  return (
    <div className={type === "scroll" ? "scroll-tabs tabs" : "tabs"}>
      {tabs.map((tab) => (
        <button key={tab.id} type="button" className={activeTab === tab.id ? "btn-tab active" : "btn-tab"} onClick={() => handleTabClick(tab.id)}>
          {tab.label}
        </button>
      ))}
    </div>
  )
}
export default Tab