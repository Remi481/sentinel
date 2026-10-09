import type { Device } from '../types'
import { formatRelative } from '../format'

interface DeviceCardProps {
  device: Device
  now: Date
  onSelect: (device: Device) => void
}

export default function DeviceCard({ device, now, onSelect }: DeviceCardProps) {
  const online = device.status === 'ONLINE'
  return (
    <button
      type="button"
      onClick={() => onSelect(device)}
      className="w-full rounded-xl border border-neutral-200 bg-white p-4 text-left shadow-sm transition-colors hover:border-neutral-300 dark:border-neutral-800 dark:bg-neutral-900 dark:hover:border-neutral-700"
    >
      <div className="flex items-start justify-between gap-3">
        <p className="min-w-0 truncate text-base font-semibold text-neutral-900 dark:text-neutral-50">
          {device.hostname}
        </p>
        <span
          className={`inline-flex shrink-0 items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-medium ${
            online
              ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-300'
              : 'bg-rose-100 text-rose-700 dark:bg-rose-900/60 dark:text-rose-300'
          }`}
        >
          <span
            className={`h-1.5 w-1.5 rounded-full ${
              online ? 'bg-emerald-500' : 'bg-rose-500'
            }`}
          />
          {device.status}
        </span>
      </div>
      <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
        {device.operatingSystem ?? 'Unknown OS'}
        {device.architecture ? ` · ${device.architecture}` : ''}
      </p>
      {device.ipAddress ? (
        <p className="mt-0.5 truncate font-mono text-xs text-neutral-500 dark:text-neutral-400">
          {device.ipAddress}
        </p>
      ) : null}
      <p className="mt-3 text-xs text-neutral-400 dark:text-neutral-500">
        Last seen {formatRelative(device.lastSeen, now)}
      </p>
    </button>
  )
}