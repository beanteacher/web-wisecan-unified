import * as React from "react"
import { X, AlertTriangle } from "lucide-react"
import { cva, type VariantProps } from "class-variance-authority"

import { cn } from "@/lib/utils"

const alertBannerVariants = cva(
  "relative flex items-center gap-3 overflow-hidden rounded-lg px-4 py-3.5",
  {
    variants: {
      variant: {
        error: "bg-red-50",
        warning: "bg-amber-50",
      },
      size: {
        default: "[&>[data-slot=alert-bar]]:w-1",
        page: "[&>[data-slot=alert-bar]]:w-1.5",
      },
    },
    defaultVariants: {
      variant: "error",
      size: "default",
    },
  }
)

const barColorMap = {
  error: "bg-red-500",
  warning: "bg-amber-500",
}

const iconBgMap = {
  error: "bg-red-500",
  warning: "bg-amber-500",
}

const textColorMap = {
  error: "text-red-900",
  warning: "text-amber-900",
}

const closeBgMap = {
  error: "bg-red-300 hover:bg-red-400",
  warning: "bg-amber-300 hover:bg-amber-400",
}

interface AlertBannerProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof alertBannerVariants> {
  message: string
  onClose?: () => void
}

function AlertBanner({
  className,
  variant = "error",
  size = "default",
  message,
  onClose,
  ...props
}: AlertBannerProps) {
  const v = variant!

  return (
    <div
      data-slot="alert-banner"
      className={cn(alertBannerVariants({ variant, size, className }))}
      {...props}
    >
      <div
        data-slot="alert-bar"
        className={cn("absolute left-0 top-0 h-full rounded-l-sm", barColorMap[v])}
      />
      <div
        className={cn(
          "flex size-6 shrink-0 items-center justify-center rounded-full text-white",
          iconBgMap[v]
        )}
      >
        <AlertTriangle className="size-3.5" />
      </div>
      <p
        className={cn(
          "flex-1",
          size === "page" ? "text-sm" : "text-[13px]",
          textColorMap[v]
        )}
      >
        {message}
      </p>
      {onClose && (
        <button
          onClick={onClose}
          className={cn(
            "flex size-5 shrink-0 items-center justify-center rounded-full text-white transition-colors",
            closeBgMap[v]
          )}
        >
          <X className="size-3" />
        </button>
      )}
    </div>
  )
}

export { AlertBanner, alertBannerVariants }
export type { AlertBannerProps }
