import { forwardRef, useId, type TextareaHTMLAttributes } from "react";
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
  // Input과 동일하게, id를 안 넘겨도 label과 textarea가 항상 연결되도록 한다.
  const generatedId = useId();
  const textareaId = id ?? generatedId;
  const errorId = error ? `${textareaId}-error` : undefined;
  return (
    <div className={`form ${className ?? ""}`}>
      <label htmlFor={textareaId} className={blind ? "blind" : "label"}>{label}</label>
      <textarea
        ref={ref}
        name={name}
        id={textareaId}
        aria-invalid={!!error}
        aria-describedby={errorId}
        {...rest}
      ></textarea>
      {error && <p className="error-msg" id={errorId}>{error}</p>}
    </div>
  )
})

Textarea.displayName = "Textarea";

export default Textarea
