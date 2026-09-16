import React, { type ElementType } from 'react'
import "./Button.css"

interface Props {
  as?: ElementType;
  href?: string;
  id?: string;
  className?: string;
  type?: "button" | "submit" | "reset";
  text: string;
  blind?: boolean;
  disabled?: boolean;
  img?: string;
  icon?: boolean;
  size?: "icon" | "sm" | "md" | "lg";
  variant?: "primary" | "secondary" | "destructive" | "negative" | "ghost" | "fixed";
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => void;
  onFocus?: (e: React.FocusEvent<HTMLButtonElement>) => void;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  [key: string]: any;
}

const Button = ({ 
  as: Tag = "button", href, id, className, type = "button", text, blind, disabled, img, icon, size = "md", variant = "primary", onClick, onFocus, ...rest 
}: Props) => {
  const isButton = Tag === "button"
  const classNames = `btn btn-${variant} btn-${size} ${icon ? "btn-icon" : ""} ${disabled ? "is-disabled" : ""} ${className ?? ""}`

  return (
    <Tag
      type={isButton ? type : undefined}
      id={id}
      href={href}
      className={classNames}
      disabled={isButton ? disabled : undefined}
      onClick={onClick}
      onFocus={onFocus}
      {...rest}
    >
      {img && (
        <img src={img} alt="" />
      )}
      <span className={blind || icon ? 'blind' : ""}>{text}</span>
    </Tag>
  )
}

export default Button