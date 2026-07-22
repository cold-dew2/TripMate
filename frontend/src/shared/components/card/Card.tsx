import React from 'react'
import "./Card.css"

interface Props {
  children?: React.ReactNode;
  className?: string;
}

const Card = ({ children, className }: Props) => {
  return (
    <div className={`card ${className ? `${className}` : ""}`}>
      {children}
    </div>
  )
}

export default Card