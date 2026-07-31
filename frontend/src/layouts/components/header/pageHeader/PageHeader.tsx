import { Link, useNavigate } from "react-router-dom";
import "./PageHeader.css"

interface PageHeaderProps {
  pageTitle?: string;
  contentTitle?: string;
  href?: string;
  linkText?: string;
  current?: number | undefined;
  total?: number | undefined;
}

const PageHeader = ({ pageTitle, contentTitle, href, linkText, current, total }: PageHeaderProps) => {
  const navigate = useNavigate();

  return (
    <div className="page-title">
      <div className="title-left">
        {contentTitle && (
          <>
            <button onClick={() => navigate(-1)}>
              <span className="bilnd">뒤로가기</span>
            </button>
            <p className="coontent-title">{contentTitle}</p>
          </>
        )}
        {pageTitle && (
          <p className="title">{pageTitle}</p>
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