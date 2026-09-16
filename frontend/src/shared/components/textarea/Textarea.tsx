import "./Input.css"

interface TextareaProps {
  id?: string;
  className?: string;
  label: string;
  blind?: boolean;
  name: string;
  placeholder?: string;
  value?: string;
  disabled?: boolean;
  readonly?: boolean;
  error?: string;  
  onChange?: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
}

const Input = ({ id, className, label, blind, name, placeholder, value, disabled, readonly, error, onChange 
}: TextareaProps) => {
  return (
    <div className={`form ${className ? `${className}` : ""}`}>
      <label htmlFor={id} className={blind ? "blind" : "label"}>{label}</label>
      <textarea name={name} id={id} placeholder={placeholder} disabled={disabled} readOnly={readonly} onChange={onChange} value={value}></textarea>
        {error && <p className="error-msg">{error}</p>}
    </div>
  )
}
export default Input