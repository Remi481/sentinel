export type DeviceStatus = 'ONLINE' | 'OFFLINE'

export interface Device {
  uuid: string
  hostname: string
  operatingSystem: string | null
  architecture: string | null
  ipAddress: string | null
  agentVersion: string | null
  status: DeviceStatus
  lastSeen: string | null
}

export interface MetricReading {
  recordedAt: string
  cpuUsagePercent: number | null
  memoryUsedBytes: number | null
  memoryTotalBytes: number | null
  diskUsedBytes: number | null
  diskTotalBytes: number | null
  uptimeSeconds: number | null
  networkReceivedBytesPerSecond: number | null
  networkSentBytesPerSecond: number | null
}