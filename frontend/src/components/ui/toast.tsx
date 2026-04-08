"use client"

import * as React from "react"
import { X, Check, AlertTriangle, Info } from "lucide-react"
import { cva, type VariantProps } from "class-variance-authority"

import { cn } from "@/lib/utils"

const toastBarVariants = cva("absolute left-0 top-0 h-full w-1 rounded-l-lg", {
  variants: {
    variant: {
      success: "bg-green-500",
      error: "bg-red-500",
      info: "bg-blue-600",
    },
  },
})

const toastIconVariants = cva(
  "flex size-6 shrink-0 items-center justify-center rounded-full text-white",
  {
    variants: {
      variant: {
        success: "bg-green-500",
        error: "bg-red-500",
        info: "bg-blue-600",
      },
    },
  }
)

const toastIcons = {
  success: Check,
  error: AlertTriangle,
  info: Info,
} as const

interface ToastProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof toastBarVariants> {
  message: string
  onClose?: () => void
}

function Toast({
  className,
  variant = "success",
  message,
  onClose,
  ...props
}: ToastProps) {
  const Icon = toastIcons[variant!]

  return (
    <div
      data-slot="toast"
      className={cn(
        "relative flex w-full max-w-[400px] items-center gap-3 overflow-hidden rounded-lg border border-slate-200 bg-white px-4 py-3.5",
        className
      )}
      {...props}
    >
      <div className={toastBarVariants({ variant })} />
      <div className={toastIconVariants({ variant })}>
        <Icon className="size-3.5" />
      </div>
      <p className="flex-1 text-[13px] text-slate-900">{message}</p>
      {onClose && (
        <button
          onClick={onClose}
          className="flex size-5 shrink-0 items-center justify-center rounded-full bg-slate-100 text-slate-400 transition-colors hover:bg-slate-200"
        >
          <X className="size-3" />
        </button>
      )}
    </div>
  )
}

export { Toast }
export type { ToastProps }
