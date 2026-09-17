import { Link } from 'react-router-dom';
import "./ContentTitle.css"

interface Props {
  title: string;
  desc?: string;
  href?: string;
  linkText?: string;
}

const ContentTitle = ({ title, desc, href, linkText }: Props) => {
  return (
    <div className="content-title">
      <div className="title-info">
        <h2 className="title">{title}</h2>
        <span className="desc">{desc}</span>
      </div>
      {href && (
        <Link to={href} className="link">{linkText}</Link>
      )}
    </div>
  )
}

export default ContentTitle