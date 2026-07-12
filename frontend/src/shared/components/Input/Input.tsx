interface InputProps {
    id?: string;
    label?: string;
    type?: string;
    error?: string;  
}

const Input = ({type = "text", id, label, error, ...props}: InputProps) => {
  return (
    <div className="form">
        <label htmlFor={id}>{label}</label>
        <input type={type} id={id} {...props}/>

        {error && <p className="error-msg">{error}</p>}
    </div>
  )
}
export default Input