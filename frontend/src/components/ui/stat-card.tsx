import * as React from "react"
import { TrendingUp, TrendingDown, Minus } from "lucide-react"

import { cn } from "@/lib/utils"

type TrendDirection = "up" | "down" | "neutral"

interface StatCardProps extends React.HTMLAttributes<HTMLDivElement> {
  label: string
  value: string
  trend?: string
  trendDirection?: TrendDirection
}

const trendConfig: Record<TrendDirection, { icon: React.ElementType; className: string }> = {
  up: { icon: TrendingUp, className: "text-green-500" },
  down: { icon: TrendingDown, className: "text-red-500" },
  neutral: { icon: Minus, className: "text-slate-500" },
}

function StatCard({
  className,
  label,
  value,
  trend,
  trendDirection = "neutral",
  ...props
}: StatCardProps) {
  const { icon: TrendIcon, className: trendClassName } = trendConfig[trendDirection]

  return (
    <div
      data-slot="stat-card"
      className={cn(
        "relative h-[120px] overflow-hidden rounded-xl border border-slate-200 bg-white p-5",
        className
      )}
      {...props}
    >
      <div className="absolute right-4 top-4 flex size-7 items-center justify-center rounded-md bg-slate-100">
        <TrendIcon className={cn("size-4", trendClassName)} />
      </div>
      <p className="text-xs font-medium text-slate-500">{label}</p>
      <p className="mt-1 text-[30px] font-bold leading-tight text-slate-900">{value}</p>
      {trend && (
        <p className={cn("mt-2 text-xs", trendClassName)}>{trend}</p>
      )}
    </div>
  )
}

export { StatCard }
export type { StatCardProps }
