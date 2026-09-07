import "./Input.css"

interface InputProps {
  id?: string;
  className?: string;
  label: string;
  blind?: boolean;
  type?: string;
  name: string;
  placeholder: string;
  value?: string;
  disabled?: boolean;
  readonly?: boolean;
  error?: string;  
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
}

const Input = ({ 
  type = "text", id, className, label, blind, name, placeholder, value, disabled, readonly, error, onChange 
}: InputProps) => {
  return (
    <div className={`form ${className ? `${className}` : ""}`}>
      <label htmlFor={id} className={blind ? "blind" : "label"}>{label}</label>
      <input type={type} id={id} placeholder={placeholder} name={name} disabled={disabled} readOnly={readonly} onChange={onChange} value={value}/>
        {error && <p className="error-msg">{error}</p>}
    </div>
  )
}
export default Input