import "./NoticeBox.css"

interface NoticeBoxProps {
  desc: string;
  type?: "primary" | "red"
}
const NoticeBox = ({ desc, type = "primary" }: NoticeBoxProps ) => {
  return (
    <div className={`notice-box ${type === "red" ? "red-box" : ""}`}>{desc}</div>
  )
}

export default NoticeBox