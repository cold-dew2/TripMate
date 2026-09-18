import { forwardRef, type InputHTMLAttributes } from "react";
import "./Input.css"

interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "name"> {
  id?: string;
  className?: string;
  label: string;
  blind?: boolean;
  name: string;
  error?: string;
}

const Input = forwardRef<HTMLInputElement, InputProps>(({
  type = "text", id, className, label, blind, name, error, ...rest
}, ref) => {
  return (
    <div className={`form ${className ? `${className}` : ""}`}>
      <label htmlFor={id} className={blind ? "blind" : "label"}>{label}</label>
      <input ref={ref} type={type} id={id} name={name} {...rest} />
      {error && <p className="error-msg">{error}</p>}
    </div>
  )
})

Input.displayName = "Input";

export default Input