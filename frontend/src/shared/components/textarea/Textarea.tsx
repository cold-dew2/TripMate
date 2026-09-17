import { forwardRef, type TextareaHTMLAttributes } from "react";
import "./Textarea.css"

interface TextareaProps extends Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, "name"> {
  id?: string;
  className?: string;
  label: string;
  blind?: boolean;
  name: string;
  error?: string;
}

const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(({
  id, className, label, blind, name, error, ...rest
}, ref) => {
  return (
    <div className={`form ${className ?? ""}`}>
      <label htmlFor={id} className={blind ? "blind" : "label"}>{label}</label>
      <textarea ref={ref} name={name} id={id} {...rest}></textarea>
      {error && <p className="error-msg">{error}</p>}
    </div>
  )
})

Textarea.displayName = "Textarea";

export default Textarea
