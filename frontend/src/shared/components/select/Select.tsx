import { forwardRef, type SelectHTMLAttributes } from 'react'
import "./Select.css"

interface OptionProps {
  value: string;
  option: string;
}

interface SelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, "name"> {
  id?: string;
  className?: string;
  label: string;
  blind?: boolean;
  name: string;
  error?: string;
  options?: OptionProps[];
  placeholder?: string;
}

const Select = forwardRef<HTMLSelectElement, SelectProps>(({
  id, className, label, blind, name, error, options, placeholder, defaultValue, ...rest
}, ref) => {
  // defaultValue를 rest에서 분리해내지 않으면, 아래 {...rest}가 이 앞의 명시적
  // defaultValue={...} 계산 결과를 그대로 덮어써 버린다(호출부가 defaultValue를
  // undefined로 넘긴 경우에도 rest 안엔 defaultValue: undefined 키가 남아있어서
  // 스프레드가 이겨버림). 그러면 select가 플레이스홀더(value="") 대신 hidden 속성이
  // 없는 첫 번째 실제 옵션을 브라우저 기본 동작으로 선택해버려, 사용자가 아무것도
  // 고르지 않았는데도 첫 옵션이 이미 선택된 것처럼 보이는 문제가 있었다.
  const effectiveDefaultValue = defaultValue ?? (placeholder ? "" : undefined);
  return (
    <div className={`form select ${className || ''}`}>
      {label && (
        <label htmlFor={id} className={blind ? 'blind' : ''}>
          {label}
        </label>
      )}
      <div className="select-control">
        <select
          ref={ref}
          id={id}
          name={name}
          defaultValue={effectiveDefaultValue}
          {...rest}
        >
          {placeholder && (
            <option value="" disabled hidden>
              {placeholder}
            </option>
          )}
          {options?.map((option) => (
            <option key={option.value} value={option.value}>
              {option.option}
            </option>
          ))}
        </select>
      </div>
      {error && <span className="error">{error}</span>}
    </div>
  )
})

Select.displayName = "Select";

export default Select
