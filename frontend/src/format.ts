const N_A_DASH = '—'

function trim(value: number): string {
  const rounded = Math.round(value * 10) / 10
  return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(1)
}

export function formatBytes(value: number | null | undefined): string {
  if (value == null || Number.isNaN(value)) return N_A_DASH
  const abs = Math.abs(value)
  if (abs < 1024) return `${Math.round(value)} B`
  if (abs < 1024 * 1024) return `${trim(value / 1024)} KB`
  if (abs < 1024 * 1024 * 1024) return `${trim(value / (1024 * 1024))} MB`
  return `${trim(value / (1024 * 1024 * 1024))} GB`
}

export function formatRate(value: number | null | undefined): string {
  const bytes = formatBytes(value)
  return bytes === N_A_DASH ? N_A_DASH : `${bytes}/s`
}

export function formatPercent(value: number | null | undefined): string {
  if (value == null || Number.isNaN(value)) return N_A_DASH
  return `${trim(value)}%`
}

export function formatUptime(value: number | null | undefined): string {
  if (value == null || Number.isNaN(value)) return N_A_DASH
  const seconds = Math.floor(value)
  if (seconds < 0) return N_A_DASH
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  if (days > 0) return `${days}d ${hours}h ${minutes}m`
  if (hours > 0) return `${hours}h ${minutes}m`
  if (minutes > 0) return `${minutes}m ${secs}s`
  return `${secs}s`
}

export function formatRelative(
  value: string | null | undefined,
  now: Date = new Date(),
): string {
  if (!value) return N_A_DASH
  const time = new Date(value).getTime()
  if (Number.isNaN(time)) return N_A_DASH
  const elapsed = Math.max(0, now.getTime() - time)
  const seconds = Math.floor(elapsed / 1000)
  if (seconds < 30) return 'just now'
  if (seconds < 60) return `${seconds}s ago`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}d ago`
  return new Date(value).toLocaleString()
}

export function computePercent(
  part: number | null | undefined,
  total: number | null | undefined,
): number | null {
  if (
    part == null ||
    total == null ||
    Number.isNaN(part) ||
    Number.isNaN(total) ||
    total <= 0
  ) {
    return null
  }
  return (part / total) * 100
}

export const EMPTY_VALUE = N_A_DASH