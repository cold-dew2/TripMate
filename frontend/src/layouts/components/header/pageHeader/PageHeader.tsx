import { Link, useNavigate } from "react-router-dom";
import "./PageHeader.css"

interface PageHeaderProps {
  pageTitle?: string;
  contentTitle?: string;
  href?: string;
  linkText?: string;
  current?: number | undefined;
  total?: number | undefined;
  onBack?: () => void;
}

const PageHeader = ({ pageTitle, contentTitle, href, linkText, current, total, onBack }: PageHeaderProps) => {
  const navigate = useNavigate();

  return (
    <div className="page-title">
      <div className="title-left">
        {contentTitle && (
          <>
            <button onClick={onBack ?? (() => navigate(-1))} className="btn-back">
              <span className="blind">뒤로가기</span>
            </button>
            <h1 className="coontent-title">{contentTitle}</h1>
          </>
        )}
        {pageTitle && (
          <h1 className="title">{pageTitle}</h1>
        )}
      </div>
      <div className="title-right">
        {href && (
          <Link to={href} className="link">{linkText}</Link>
        )}
        {current && total && (
          <p className="progress">{current}/{total}</p>
        )}
      </div>
    </div>
  )
}  

export default PageHeader