import { clearToken, expireSession, getToken } from './auth'
import type { Device, MetricReading } from './types'

export class DeviceNotFoundError extends Error {
  readonly uuid: string

  constructor(uuid: string) {
    super(`Device not found: ${uuid}`)
    this.name = 'DeviceNotFoundError'
    this.uuid = uuid
  }
}

export class UnauthorizedError extends Error {
  constructor() {
    super('Unauthorized')
    this.name = 'UnauthorizedError'
  }
}

export class InvalidCredentialsError extends Error {
  constructor() {
    super('Invalid username or password')
    this.name = 'InvalidCredentialsError'
  }
}

function authHeaders(): Record<string, string> {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function guardedFetch(
  input: string,
  init: RequestInit = {},
): Promise<Response> {
  const response = await fetch(input, init)
  if (response.status === 401) {
    expireSession()
    throw new UnauthorizedError()
  }
  return response
}

export async function login(
  username: string,
  password: string,
  baseUrl = '',
): Promise<string> {
  const response = await fetch(`${baseUrl}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  if (response.status === 401) {
    clearToken()
    throw new InvalidCredentialsError()
  }
  if (!response.ok) {
    throw new Error(`Login failed (HTTP ${response.status})`)
  }
  const body = (await response.json()) as { token: string }
  return body.token
}

export async function fetchDevices(baseUrl = ''): Promise<Device[]> {
  const response = await guardedFetch(`${baseUrl}/api/v1/devices`, {
    headers: authHeaders(),
  })
  if (!response.ok) {
    throw new Error(`Failed to load devices (HTTP ${response.status})`)
  }
  return (await response.json()) as Device[]
}

export async function fetchDevice(
  uuid: string,
  baseUrl = '',
): Promise<Device> {
  const response = await guardedFetch(
    `${baseUrl}/api/v1/devices/${encodeURIComponent(uuid)}`,
    { headers: authHeaders() },
  )
  if (response.status === 404) {
    throw new DeviceNotFoundError(uuid)
  }
  if (!response.ok) {
    throw new Error(`Failed to load device (HTTP ${response.status})`)
  }
  return (await response.json()) as Device
}

export async function fetchMetrics(
  uuid: string,
  baseUrl = '',
): Promise<MetricReading[]> {
  const response = await guardedFetch(
    `${baseUrl}/api/v1/devices/${encodeURIComponent(uuid)}/metrics`,
    { headers: authHeaders() },
  )
  if (response.status === 404) {
    throw new DeviceNotFoundError(uuid)
  }
  if (!response.ok) {
    throw new Error(`Failed to load metrics (HTTP ${response.status})`)
  }
  return (await response.json()) as MetricReading[]
}

export async function revokeDeviceToken(
  uuid: string,
  baseUrl = '',
): Promise<void> {
  const response = await guardedFetch(
    `${baseUrl}/api/v1/devices/${encodeURIComponent(uuid)}/revoke-token`,
    { method: 'POST', headers: authHeaders() },
  )
  if (!response.ok) {
    throw new Error(`Failed to revoke device token (HTTP ${response.status})`)
  }
}

export { clearToken }
