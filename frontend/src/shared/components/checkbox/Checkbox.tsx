import React, { type InputHTMLAttributes } from 'react'
import "./Checkbox.css"

interface CheckboxProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  icon?: string;
}

const Checkbox = ({ label, icon, id, ...props }: CheckboxProps) => {
  return (
    <div className="checkbox">
      <input type="checkbox" id={id} {...props} className="blind" />
      <label htmlFor={id}>
        {icon && <img src={`/icons/${icon}`} alt={label} className="icon"/>}
        {label && <span className="label-text">{label}</span>}
      </label>
    </div>
  )
}

export default Checkbox