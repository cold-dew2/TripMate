import { forwardRef, useId, type InputHTMLAttributes } from "react";
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
  // id를 안 넘긴 호출부가 많아 label의 htmlFor가 짝을 못 찾는 경우가 있었다(스크린
  // 리더가 라벨과 입력을 연결 못 하고, 라벨 클릭으로 포커스 이동도 안 됐다). 항상
  // 유효한 id가 있도록 useId()로 기본값을 만든다.
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const errorId = error ? `${inputId}-error` : undefined;
  return (
    <div className={`form ${className ? `${className}` : ""}`}>
      <label htmlFor={inputId} className={blind ? "blind" : "label"}>{label}</label>
      <input
        ref={ref}
        type={type}
        id={inputId}
        name={name}
        aria-invalid={!!error}
        aria-describedby={errorId}
        {...rest}
      />
      {error && <p className="error-msg" id={errorId}>{error}</p>}
    </div>
  )
})

Input.displayName = "Input";

export default Input