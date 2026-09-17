import { Link } from "react-router-dom";
import "./RegionBanner.css";

interface Props {
  eyebrow: string;
  title: string;
  desc?: string;
  href: string;
  icon?: string;
  compact?: boolean;
}

const RegionBanner = ({ eyebrow, title, desc, href, icon = "🏞️", compact }: Props) => {
  return (
    <Link to={href} className={`region-banner${compact ? " region-banner-compact" : ""}`}>
      <div className="region-banner-text">
        <p className="region-banner-eyebrow">{eyebrow}</p>
        <p className="region-banner-title">{title}</p>
        {desc && <p className="region-banner-desc">{desc}</p>}
      </div>
      <span className="region-banner-icon" aria-hidden="true">{icon}</span>
    </Link>
  );
};

export default RegionBanner;
