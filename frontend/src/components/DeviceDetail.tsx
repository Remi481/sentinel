import { useEffect, useMemo, useState } from 'react'
import { DeviceNotFoundError, fetchMetrics } from '../api'
import {
  formatBytes,
  formatPercent,
  formatRelative,
  formatRate,
  formatUptime,
  computePercent,
} from '../format'
import type { Device, DeviceStatus, MetricReading } from '../types'
import MetricChart, { type ChartPoint } from './MetricChart'
import StatTile from './StatTile'

const METRICS_POLL_MS = 5000

const CPU_COLOR = '#0ea5e9'
const MEMORY_COLOR = '#8b5cf6'
const DISK_COLOR = '#f59e0b'
const UPTIME_COLOR = '#10b981'
const RX_COLOR = '#06b6d4'
const TX_COLOR = '#f43f5e'

interface DeviceDetailProps {
  device: Device
  onBack: () => void
}

interface ChartRow {
  t: number
  cpu: number | null
  memory: number | null
  disk: number | null
  uptime: number | null
  rx: number | null
  tx: number | null
}

function StatusBadge({ status }: { status: DeviceStatus }) {
  const online = status === 'ONLINE'
  return (
    <span
      className={`inline-flex shrink-0 items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-medium ${
        online
          ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-300'
          : 'bg-rose-100 text-rose-700 dark:bg-rose-900/60 dark:text-rose-300'
      }`}
    >
      <span
        className={`h-1.5 w-1.5 rounded-full ${online ? 'bg-emerald-500' : 'bg-rose-500'}`}
      />
      {status}
    </span>
  )
}

function uptimeFormatter(hours: number): string {
  const rounded = Math.round(hours * 10) / 10
  return `${Number.isInteger(rounded) ? rounded : rounded.toFixed(1)} h`
}

export default function DeviceDetail({ device, onBack }: DeviceDetailProps) {
  const [metrics, setMetrics] = useState<MetricReading[] | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState(false)

  useEffect(() => {
    let cancelled = false
    let timeout: number | undefined
    let inFlight = false

    async function poll() {
      if (cancelled || inFlight) return
      inFlight = true
      try {
        const data = await fetchMetrics(device.uuid)
        if (cancelled) return
        setMetrics(data)
        setNotFound(false)
        setError(false)
      } catch (err) {
        if (cancelled) return
        if (err instanceof DeviceNotFoundError) {
          setNotFound(true)
        } else {
          setError(true)
        }
      } finally {
        inFlight = false
        if (!cancelled) schedule(METRICS_POLL_MS)
      }
    }

    function schedule(delay: number) {
      if (document.hidden) return
      timeout = window.setTimeout(() => {
        timeout = undefined
        void poll()
      }, delay)
    }

    function onVisibility() {
      if (!document.hidden && timeout == null) schedule(0)
    }

    void poll()
    document.addEventListener('visibilitychange', onVisibility)
    return () => {
      cancelled = true
      document.removeEventListener('visibilitychange', onVisibility)
      window.clearTimeout(timeout)
    }
  }, [device.uuid])

  const rows = useMemo<ChartRow[]>(() => {
    if (!metrics) return []
    return [...metrics].reverse().map((m) => ({
      t: new Date(m.recordedAt).getTime(),
      cpu: m.cpuUsagePercent,
      memory: computePercent(m.memoryUsedBytes, m.memoryTotalBytes),
      disk: computePercent(m.diskUsedBytes, m.diskTotalBytes),
      uptime: m.uptimeSeconds == null ? null : m.uptimeSeconds / 3600,
      rx: m.networkReceivedBytesPerSecond,
      tx: m.networkSentBytesPerSecond,
    }))
  }, [metrics])

  const cpuPoints: ChartPoint[] = rows.map((r) => ({ t: r.t, v: r.cpu }))
  const memoryPoints: ChartPoint[] = rows.map((r) => ({ t: r.t, v: r.memory }))
  const diskPoints: ChartPoint[] = rows.map((r) => ({ t: r.t, v: r.disk }))
  const uptimePoints: ChartPoint[] = rows.map((r) => ({ t: r.t, v: r.uptime }))
  const networkPoints: ChartPoint[] = rows.map((r) => ({
    t: r.t,
    v: r.rx,
    v2: r.tx,
  }))

  const latest = metrics && metrics.length > 0 ? metrics[0] : null
  const memoryPercent = computePercent(
    latest?.memoryUsedBytes,
    latest?.memoryTotalBytes,
  )
  const diskPercent = computePercent(latest?.diskUsedBytes, latest?.diskTotalBytes)

  if (notFound) {
    return (
      <div className="mx-auto max-w-md rounded-xl border border-neutral-200 bg-white p-8 text-center dark:border-neutral-800 dark:bg-neutral-900">
        <h2 className="text-lg font-semibold text-neutral-900 dark:text-neutral-50">
          Device not found
        </h2>
        <p className="mt-1 text-sm text-neutral-500 dark:text-neutral-400">
          The device you were viewing no longer exists.
        </p>
        <button
          type="button"
          onClick={onBack}
          className="mt-4 rounded-lg bg-sky-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-sky-500"
        >
          Back to devices
        </button>
      </div>
    )
  }

  return (
    <div>
      <button
        type="button"
        onClick={onBack}
        className="mb-3 text-sm font-medium text-sky-600 transition-colors hover:text-sky-500 dark:text-sky-400"
      >
        &larr; Back to devices
      </button>

      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-3">
            <h2 className="text-xl font-semibold text-neutral-900 dark:text-neutral-50">
              {device.hostname}
            </h2>
            <StatusBadge status={device.status} />
          </div>
          <p className="mt-1 text-sm text-neutral-500 dark:text-neutral-400">
            {device.operatingSystem ?? 'Unknown OS'}
            {device.architecture ? ` · ${device.architecture}` : ''}
            {device.ipAddress ? ` · ${device.ipAddress}` : ''}
            {device.agentVersion ? ` · agent v${device.agentVersion}` : ''}
          </p>
          <p className="mt-0.5 text-xs text-neutral-400 dark:text-neutral-500">
            Last seen {formatRelative(device.lastSeen)}
          </p>
        </div>
      </div>

      {error ? (
        <div className="mb-4 rounded-lg border border-amber-300 bg-amber-50 px-4 py-2 text-sm text-amber-800 dark:border-amber-800 dark:bg-amber-950/40 dark:text-amber-200">
          Cannot reach the Sentinel API
        </div>
      ) : null}

      {metrics === null ? (
        <div>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 xl:grid-cols-4">
            {Array.from({ length: 6 }, (_, i) => (
              <div
                key={i}
                className="h-28 animate-pulse rounded-xl bg-neutral-200 dark:bg-neutral-800"
              />
            ))}
          </div>
          <div className="mt-6 grid gap-6 lg:grid-cols-2">
            {Array.from({ length: 4 }, (_, i) => (
              <div
                key={i}
                className="h-[260px] animate-pulse rounded-xl bg-neutral-200 dark:bg-neutral-800"
              />
            ))}
          </div>
        </div>
      ) : metrics.length === 0 ? (
        <div className="rounded-xl border border-dashed border-neutral-300 p-10 text-center text-sm text-neutral-500 dark:border-neutral-700 dark:text-neutral-400">
          No readings yet for this device. The agent will start reporting metrics
          soon.
        </div>
      ) : (
        <div>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 xl:grid-cols-4">
            <StatTile
              label="CPU"
              value={formatPercent(latest?.cpuUsagePercent ?? null)}
            />
            <StatTile
              label="Memory"
              value={formatPercent(memoryPercent)}
              sub={`${formatBytes(latest?.memoryUsedBytes)} of ${formatBytes(
                latest?.memoryTotalBytes,
              )}`}
            />
            <StatTile
              label="Disk"
              value={formatPercent(diskPercent)}
              sub={`${formatBytes(latest?.diskUsedBytes)} of ${formatBytes(
                latest?.diskTotalBytes,
              )}`}
            />
            <StatTile
              label="Uptime"
              value={formatUptime(latest?.uptimeSeconds ?? null)}
            />
            <StatTile
              label="Network RX"
              value={formatRate(latest?.networkReceivedBytesPerSecond ?? null)}
            />
            <StatTile
              label="Network TX"
              value={formatRate(latest?.networkSentBytesPerSecond ?? null)}
            />
          </div>

          <div className="mt-6 grid gap-6 lg:grid-cols-2">
            <MetricChart
              title="CPU usage"
              points={cpuPoints}
              series={[
                { key: 'v', label: 'CPU', color: CPU_COLOR },
              ]}
              formatter={(value) => formatPercent(value)}
              max={100}
            />
            <MetricChart
              title="Memory usage"
              points={memoryPoints}
              series={[
                { key: 'v', label: 'Memory', color: MEMORY_COLOR },
              ]}
              formatter={(value) => formatPercent(value)}
              max={100}
            />
            <MetricChart
              title="Disk usage"
              points={diskPoints}
              series={[{ key: 'v', label: 'Disk', color: DISK_COLOR }]}
              formatter={(value) => formatPercent(value)}
              max={100}
            />
            <MetricChart
              title="Uptime"
              points={uptimePoints}
              series={[
                { key: 'v', label: 'Uptime', color: UPTIME_COLOR },
              ]}
              formatter={uptimeFormatter}
            />
            <MetricChart
              title="Network throughput"
              points={networkPoints}
              series={[
                { key: 'v', label: 'Received', color: RX_COLOR },
                { key: 'v2', label: 'Sent', color: TX_COLOR },
              ]}
              formatter={(value) => formatRate(value)}
            />
          </div>
        </div>
      )}
    </div>
  )
}