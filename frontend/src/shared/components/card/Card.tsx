import React from 'react'
import "./Card.css"

interface Props {
  children?: React.ReactNode;
  className?: string;
  empty?: string;
  error?: string;
}

const Card = ({ children, className, empty, error }: Props) => {

  if (empty) {
    return (
      <div className={`card ${className ? `${className}` : ""} ${empty ? `card-empty` : ""}`}>
        <span>{empty}</span>
      </div>
    )
  }

  if (error) {
    return (
      <div className={`card ${className ? `${className}` : ""} ${error ? `card-empty` : ""}`}>
        <span>{error}</span>
      </div>
    )
  }


  return (
    <div className={`card ${className ? `${className}` : ""}`}>
      {children}
    </div>
  )
}

export default Card