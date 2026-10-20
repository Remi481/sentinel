// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { fetchDevice, fetchDevices, fetchMetrics } from './api'
import { setToken } from './auth'
import type { Device } from './types'

vi.mock('./api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./api')>()
  return {
    ...actual,
    fetchDevices: vi.fn(),
    fetchDevice: vi.fn(),
    fetchMetrics: vi.fn(),
    revokeDeviceToken: vi.fn(),
  }
})

const mockedFetchDevices = vi.mocked(fetchDevices)
const mockedFetchDevice = vi.mocked(fetchDevice)
const mockedFetchMetrics = vi.mocked(fetchMetrics)

function makeDevice(overrides: Partial<Device> = {}): Device {
  return {
    uuid: 'uuid-a',
    hostname: 'alpha',
    operatingSystem: 'Linux',
    architecture: 'x86_64',
    ipAddress: '10.0.0.1',
    agentVersion: '1.0.0',
    status: 'ONLINE',
    lastSeen: '2026-10-20T12:00:00Z',
    ...overrides,
  }
}

beforeEach(() => {
  setToken('test-jwt')
  mockedFetchDevices.mockReset()
  mockedFetchDevice.mockReset()
  mockedFetchMetrics.mockReset()
})

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

describe('App device selection', () => {
  it('never shows the previous device record when opening a different device', async () => {
    const alpha = makeDevice()
    const bravo = makeDevice({ uuid: 'uuid-b', hostname: 'bravo' })

    mockedFetchDevices.mockResolvedValue([alpha, bravo])
    // The first device's poll "refreshes" its record with a new hostname; that
    // stale copy must never bleed into the next device's detail view.
    mockedFetchDevice.mockImplementation(async (uuid) =>
      uuid === alpha.uuid
        ? makeDevice({ hostname: 'alpha-updated' })
        : bravo,
    )
    mockedFetchMetrics.mockResolvedValue([])

    render(<App />)

    fireEvent.click(await screen.findByRole('button', { name: /alpha/ }))
    expect(
      await screen.findByRole('heading', { name: 'alpha-updated' }),
    ).toBeTruthy()

    fireEvent.click(
      screen.getByRole('button', { name: /back to devices/i }),
    )
    fireEvent.click(await screen.findByRole('button', { name: /bravo/ }))

    expect(
      await screen.findByRole('heading', { name: 'bravo' }),
    ).toBeTruthy()
    expect(screen.queryByText(/alpha-updated/)).toBeNull()
  })
})
