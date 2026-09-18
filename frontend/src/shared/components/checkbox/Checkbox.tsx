import { forwardRef, type InputHTMLAttributes } from 'react'
import "./Checkbox.css"

interface CheckboxProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  icon?: string;
}

const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(({ label, icon, id, ...props }, ref) => {
  return (
    <div className="checkbox">
      <input ref={ref} type="checkbox" id={id} {...props} className="blind" />
      <label htmlFor={id}>
        {icon && <img src={`/icons/${icon}`} alt={label} className="icon"/>}
        {label && <span className="label-text">{label}</span>}
      </label>
    </div>
  )
})

Checkbox.displayName = "Checkbox";

export default Checkbox
