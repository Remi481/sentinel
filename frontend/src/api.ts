import type { Device, MetricReading } from './types'

export class DeviceNotFoundError extends Error {
  readonly uuid: string

  constructor(uuid: string) {
    super(`Device not found: ${uuid}`)
    this.name = 'DeviceNotFoundError'
    this.uuid = uuid
  }
}

export async function fetchDevices(baseUrl = ''): Promise<Device[]> {
  const response = await fetch(`${baseUrl}/api/v1/devices`)
  if (!response.ok) {
    throw new Error(`Failed to load devices (HTTP ${response.status})`)
  }
  return (await response.json()) as Device[]
}

export async function fetchMetrics(
  uuid: string,
  baseUrl = '',
): Promise<MetricReading[]> {
  const response = await fetch(
    `${baseUrl}/api/v1/devices/${encodeURIComponent(uuid)}/metrics`,
  )
  if (response.status === 404) {
    throw new DeviceNotFoundError(uuid)
  }
  if (!response.ok) {
    throw new Error(`Failed to load metrics (HTTP ${response.status})`)
  }
  return (await response.json()) as MetricReading[]
}