interface MoimListProps {
  moimTitle: string;
  moimStartDt: string;
  applicantCount: number;
}

const MoimList = ({moimTitle, moimStartDt, applicantCount}: MoimListProps ) => {
  return (
    <ul className="moim-list">
      <li>
        <div className="info-left">
          <p className="info-title"></p>
        </div>
      </li>
    </ul>
  )
}
export default MoimList