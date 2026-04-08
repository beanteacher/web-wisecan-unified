"use client"

import * as React from "react"

import { cn } from "@/lib/utils"

interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  maxLength?: number
  showCount?: boolean
}

function Textarea({
  className,
  maxLength,
  showCount = false,
  value,
  defaultValue,
  onChange,
  ...props
}: TextareaProps) {
  const [internalValue, setInternalValue] = React.useState(
    (defaultValue as string) ?? ""
  )
  const isControlled = value !== undefined
  const currentValue = isControlled ? (value as string) : internalValue

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    if (!isControlled) {
      setInternalValue(e.target.value)
    }
    onChange?.(e)
  }

  return (
    <div data-slot="textarea-wrapper" className="relative">
      <textarea
        data-slot="textarea"
        className={cn(
          "min-h-[120px] w-full resize-y rounded-lg border border-slate-200 bg-white px-3 py-3 text-sm text-slate-900 outline-none transition-colors placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/50 disabled:cursor-not-allowed disabled:bg-slate-50 disabled:opacity-50 aria-invalid:border-red-500",
          showCount && maxLength && "pb-8",
          className
        )}
        maxLength={maxLength}
        value={isControlled ? value : undefined}
        defaultValue={!isControlled ? defaultValue : undefined}
        onChange={handleChange}
        {...props}
      />
      {showCount && maxLength && (
        <span className="absolute bottom-2 right-3 text-xs text-slate-400">
          {currentValue.length}/{maxLength}
        </span>
      )}
    </div>
  )
}

export { Textarea }
export type { TextareaProps }
