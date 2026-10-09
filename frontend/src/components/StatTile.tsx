interface StatTileProps {
  label: string
  value: string
  sub?: string
}

export default function StatTile({ label, value, sub }: StatTileProps) {
  return (
    <div className="rounded-xl border border-neutral-200 bg-white p-4 dark:border-neutral-800 dark:bg-neutral-900">
      <p className="text-xs font-medium uppercase tracking-wide text-neutral-500 dark:text-neutral-400">
        {label}
      </p>
      <p className="mt-1 text-2xl font-semibold text-neutral-900 dark:text-neutral-50">
        {value}
      </p>
      {sub ? (
        <p className="mt-0.5 text-sm text-neutral-500 dark:text-neutral-400">{sub}</p>
      ) : null}
    </div>
  )
}