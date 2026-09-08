import React from 'react'

interface SelectProps {
  id?: string;
  className?: string;
  label: string;
  blind?: boolean;
  name: string;
  value?: string;
  disabled?: boolean;
  error?: string;  
  options?: OptionProps[];
  onChange?: (e: React.ChangeEvent<HTMLSelectElement>) => void;
}

interface OptionProps {
  value: string;
  option: string;
}

const Select = ({ id, className, label, blind, name, value, disabled, error, options, onChange }: SelectProps) => {
  
  return (
    <div className={`form select ${className || ''}`}>
      {label && (
        <label htmlFor={id} className={blind ? 'blind' : ''}>
          {label}
        </label>
      )}
      <select
        id={id}
        name={name}
        value={value}
        disabled={disabled}
        onChange={onChange}
      >
        {options?.map((option) => (
          <option key={option.value} value={option.value}>
            {option.option}
          </option>
        ))}
      </select>
      {error && <span className="error">{error}</span>}
    </div>
  )
}

export default Select