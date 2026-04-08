import * as React from "react"

import { cn } from "@/lib/utils"

interface FormFieldProps extends React.HTMLAttributes<HTMLDivElement> {
  label?: string
  required?: boolean
  error?: string
  hint?: string
  htmlFor?: string
}

function FormField({
  className,
  label,
  required,
  error,
  hint,
  htmlFor,
  children,
  ...props
}: FormFieldProps) {
  return (
    <div data-slot="form-field" className={cn("space-y-1.5", className)} {...props}>
      {label && (
        <label
          htmlFor={htmlFor}
          className="text-sm font-medium text-slate-900"
        >
          {label}
          {required && <span className="text-red-500"> *</span>}
        </label>
      )}
      {children}
      {error && (
        <p className="text-[13px] text-red-500">{error}</p>
      )}
      {hint && !error && (
        <p className="text-xs text-slate-500">{hint}</p>
      )}
    </div>
  )
}

export { FormField }
export type { FormFieldProps }
