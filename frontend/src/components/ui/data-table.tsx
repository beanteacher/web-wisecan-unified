import * as React from "react"

import { cn } from "@/lib/utils"

function DataTable({
  className,
  ...props
}: React.HTMLAttributes<HTMLTableElement>) {
  return (
    <div data-slot="data-table" className="w-full overflow-x-auto">
      <table
        className={cn("w-full border-collapse text-sm", className)}
        {...props}
      />
    </div>
  )
}

function DataTableHeader({
  className,
  ...props
}: React.HTMLAttributes<HTMLTableSectionElement>) {
  return (
    <thead
      data-slot="data-table-header"
      className={cn("bg-slate-50", className)}
      {...props}
    />
  )
}

function DataTableHeaderCell({
  className,
  ...props
}: React.ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      data-slot="data-table-header-cell"
      className={cn(
        "h-11 border-r border-slate-200 px-4 text-center text-[11px] font-semibold uppercase text-slate-500 last:border-r-0",
        className
      )}
      {...props}
    />
  )
}

function DataTableBody({
  className,
  ...props
}: React.HTMLAttributes<HTMLTableSectionElement>) {
  return (
    <tbody
      data-slot="data-table-body"
      className={cn("[&_tr:last-child]:border-0", className)}
      {...props}
    />
  )
}

function DataTableRow({
  className,
  ...props
}: React.HTMLAttributes<HTMLTableRowElement>) {
  return (
    <tr
      data-slot="data-table-row"
      className={cn(
        "h-14 border-b border-slate-100 bg-white transition-colors hover:bg-slate-50/50",
        className
      )}
      {...props}
    />
  )
}

function DataTableCell({
  className,
  ...props
}: React.TdHTMLAttributes<HTMLTableCellElement>) {
  return (
    <td
      data-slot="data-table-cell"
      className={cn(
        "border-r border-slate-200 px-4 text-center text-sm text-slate-900 last:border-r-0",
        className
      )}
      {...props}
    />
  )
}

function DataTableFooter({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      data-slot="data-table-footer"
      className={cn(
        "flex items-center justify-between px-0 py-3 text-sm",
        className
      )}
      {...props}
    />
  )
}

export {
  DataTable,
  DataTableHeader,
  DataTableHeaderCell,
  DataTableBody,
  DataTableRow,
  DataTableCell,
  DataTableFooter,
}
