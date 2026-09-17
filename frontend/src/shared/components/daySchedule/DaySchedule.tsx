interface DayScheduleProps {
  day: string;
  date: string;
  mode?: "view" | "edit"
}

const DaySchedule = ({day, date, mode="view"}: DayScheduleProps) => {
  return (
    <div className="daySchedule">
      <div className="schedule-title">day {day} ({date}) </div>

      {mode === "view" && (
        
      )}
    </div>
  )
}
export default DaySchedule