import type { Device } from '../types'
import DeviceCard from './DeviceCard'

interface DeviceListProps {
  devices: Device[]
  now: Date
  onSelect: (device: Device) => void
}

export default function DeviceList({ devices, now, onSelect }: DeviceListProps) {
  const onlineCount = devices.filter((d) => d.status === 'ONLINE').length
  return (
    <section>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-lg font-semibold text-neutral-900 dark:text-neutral-50">
          Devices
        </h2>
        <p className="text-sm text-neutral-500 dark:text-neutral-400">
          {onlineCount} of {devices.length} online
        </p>
      </div>
      <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {devices.map((device) => (
          <li key={device.uuid}>
            <DeviceCard
              device={device}
              now={now}
              onSelect={onSelect}
            />
          </li>
        ))}
      </ul>
    </section>
  )
}