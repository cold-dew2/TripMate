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
  id, className, label, blind, name, error, options, placeholder, ...rest
}, ref) => {
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
          defaultValue={rest.defaultValue ?? (placeholder ? "" : undefined)}
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
