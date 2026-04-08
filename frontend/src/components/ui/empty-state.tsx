import * as React from "react"
import { cn } from "@/lib/utils"

interface EmptyStateProps extends React.HTMLAttributes<HTMLDivElement> {
  icon?: React.ReactNode
  title: string
  description?: string
  action?: React.ReactNode
}

function EmptyState({
  className,
  icon,
  title,
  description,
  action,
  ...props
}: EmptyStateProps) {
  return (
    <div
      data-slot="empty-state"
      className={cn(
        "flex flex-col items-center rounded-xl border border-slate-200 bg-white px-6 py-10 text-center",
        className
      )}
      {...props}
    >
      {icon ? (
        <div className="mb-4">{icon}</div>
      ) : (
        <div className="mb-4 flex size-16 items-center justify-center rounded-full bg-slate-100">
          <div className="size-8 rounded-lg bg-slate-300" />
        </div>
      )}
      <h3 className="text-base font-semibold text-slate-900">{title}</h3>
      {description && (
        <p className="mt-1 max-w-[352px] text-sm leading-relaxed text-slate-500">
          {description}
        </p>
      )}
      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}

export { EmptyState }
export type { EmptyStateProps }
