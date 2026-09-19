import { useTranslation } from "react-i18next";
import { NavLink } from "react-router-dom";
import useUser from "@/shared/hooks/useUser";
import useChatUnreadCount from "@/shared/hooks/useChatUnreadCount";
import "./Navigation.css"

const Navigation = () => {
  const { t } = useTranslation();
  const { data: user } = useUser();
  const { data: chatUnreadCount = 0 } = useChatUnreadCount();

  const menus = [
    {id: 0, path: "/", label: "nav.home", icon: "home"},
    { id: 1, path: "/placeList", label: "nav.places", icon: "place"},
    { id: 2, path: "/moimList", label: "nav.groups", icon: "group"},
    // 채팅은 로그인했을 때만 보여준다(비로그인 상태로 들어가면 목록을 볼 수 없으므로).
    ...(user ? [{ id: 3, path: "/chat", label: "nav.chat", icon: "chat" }] : []),
    { id: 4, path: "/my", label: "nav.my", icon: "my"},
  ]
  return (
    <nav>
      <ul className="menu-list">
        {menus.map(menu => (
          <li key={menu.id}>
            <NavLink to={menu.path} className={({ isActive }) => isActive ? "nav-link active" : "nav-link"}>
              <span className="icon">
                <img src={`/icons/icon-${menu.icon}.png`} alt={`${menu.icon}`} />
                {menu.icon === "chat" && chatUnreadCount > 0 && (
                  <span className="nav-badge">{chatUnreadCount > 99 ? "99+" : chatUnreadCount}</span>
                )}
              </span>
              <span className="title">{t(menu.label)}</span>
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}

export default Navigation