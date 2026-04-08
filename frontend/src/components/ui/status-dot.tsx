import { cva, type VariantProps } from "class-variance-authority"

import { cn } from "@/lib/utils"

const statusDotVariants = cva("size-2 rounded-full shrink-0", {
  variants: {
    status: {
      success: "bg-green-500",
      error: "bg-red-500",
      warning: "bg-amber-500",
      neutral: "bg-slate-400",
    },
  },
  defaultVariants: {
    status: "neutral",
  },
})

interface StatusDotProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof statusDotVariants> {
  label?: string
}

function StatusDot({ className, status, label, ...props }: StatusDotProps) {
  return (
    <span
      data-slot="status-dot"
      className={cn("inline-flex items-center gap-1.5", className)}
      {...props}
    >
      <span className={statusDotVariants({ status })} />
      {label && <span className="text-sm text-slate-900">{label}</span>}
    </span>
  )
}

export { StatusDot, statusDotVariants }
export type { StatusDotProps }
