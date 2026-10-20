// @vitest-environment jsdom
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import DeviceDetail from './DeviceDetail'
import { DeviceNotFoundError, fetchDevice, fetchMetrics } from '../api'
import type { Device, MetricReading } from '../types'

vi.mock('../api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api')>()
  return {
    ...actual,
    fetchDevice: vi.fn(),
    fetchMetrics: vi.fn(),
    revokeDeviceToken: vi.fn(),
  }
})

const mockedFetchDevice = vi.mocked(fetchDevice)
const mockedFetchMetrics = vi.mocked(fetchMetrics)

function makeDevice(overrides: Partial<Device> = {}): Device {
  return {
    uuid: 'uuid-1',
    hostname: 'alpha',
    operatingSystem: 'Linux',
    architecture: 'x86_64',
    ipAddress: '10.0.0.1',
    agentVersion: '1.0.0',
    status: 'ONLINE',
    lastSeen: '2026-10-20T11:57:00Z',
    ...overrides,
  }
}

function makeReading(overrides: Partial<MetricReading> = {}): MetricReading {
  return {
    recordedAt: '2026-10-20T11:59:00Z',
    cpuUsagePercent: 10,
    memoryUsedBytes: 1024,
    memoryTotalBytes: 2048,
    diskUsedBytes: 1024,
    diskTotalBytes: 2048,
    uptimeSeconds: 90,
    networkReceivedBytesPerSecond: 1,
    networkSentBytesPerSecond: 2,
    ...overrides,
  }
}

function lastSeenText(): string {
  return screen.getByTestId('device-last-seen').textContent ?? ''
}

async function flushInitialPoll() {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(0)
  })
}

beforeEach(() => {
  vi.useFakeTimers({
    now: new Date('2026-10-20T12:00:00Z'),
    shouldAdvanceTime: false,
  })
  mockedFetchDevice.mockReset()
  mockedFetchMetrics.mockReset()
})

afterEach(() => {
  cleanup()
  vi.useRealTimers()
  vi.restoreAllMocks()
})

describe('DeviceDetail device refresh', () => {
  it('refreshes the device record on the same polling cycle as the metrics', async () => {
    mockedFetchDevice.mockResolvedValue(
      makeDevice({ lastSeen: '2026-10-20T12:00:00Z' }),
    )
    mockedFetchMetrics.mockResolvedValue([makeReading()])

    render(<DeviceDetail device={makeDevice()} onBack={() => {}} />)
    expect(lastSeenText()).toContain('3m ago')

    await flushInitialPoll()

    expect(lastSeenText()).toContain('just now')
    expect(mockedFetchDevice).toHaveBeenCalledWith('uuid-1')
    expect(mockedFetchMetrics).toHaveBeenCalledWith('uuid-1')
    expect(mockedFetchDevice.mock.invocationCallOrder[0]).toBeLessThan(
      mockedFetchMetrics.mock.invocationCallOrder[0],
    )
  })

  it('never starts a new cycle while a request is still in flight', async () => {
    mockedFetchDevice.mockImplementation(
      () => new Promise(() => {}),
    )
    mockedFetchMetrics.mockResolvedValue([])

    render(<DeviceDetail device={makeDevice()} onBack={() => {}} />)
    await flushInitialPoll()
    expect(mockedFetchDevice).toHaveBeenCalledTimes(1)

    await act(async () => {
      await vi.advanceTimersByTime(15000)
    })

    expect(mockedFetchDevice).toHaveBeenCalledTimes(1)
    expect(mockedFetchMetrics).not.toHaveBeenCalled()
  })

  it('fetches the device and metrics sequentially within one cycle', async () => {
    let resolveDevice!: (device: Device) => void
    mockedFetchDevice.mockImplementation(
      () =>
        new Promise<Device>((resolve) => {
          resolveDevice = resolve
        }),
    )
    mockedFetchMetrics.mockResolvedValue([])

    render(<DeviceDetail device={makeDevice()} onBack={() => {}} />)
    await flushInitialPoll()

    expect(mockedFetchMetrics).not.toHaveBeenCalled()

    await act(async () => {
      resolveDevice(makeDevice())
      await vi.advanceTimersByTimeAsync(0)
    })

    expect(mockedFetchMetrics).toHaveBeenCalledTimes(1)
  })

  it('stops polling while the tab is hidden and resumes when visible', async () => {
    mockedFetchDevice.mockResolvedValue(makeDevice())
    mockedFetchMetrics.mockResolvedValue([])

    render(<DeviceDetail device={makeDevice()} onBack={() => {}} />)
    await flushInitialPoll()
    expect(mockedFetchDevice).toHaveBeenCalledTimes(1)

    Object.defineProperty(document, 'hidden', {
      configurable: true,
      get: () => true,
    })
    await act(async () => {
      document.dispatchEvent(new Event('visibilitychange'))
      await vi.advanceTimersByTime(20000)
    })
    expect(mockedFetchDevice).toHaveBeenCalledTimes(1)

    Object.defineProperty(document, 'hidden', {
      configurable: true,
      get: () => false,
    })
    await act(async () => {
      document.dispatchEvent(new Event('visibilitychange'))
      await vi.advanceTimersByTimeAsync(0)
    })
    expect(mockedFetchDevice).toHaveBeenCalledTimes(2)
  })

  it('stops polling after unmount', async () => {
    mockedFetchDevice.mockResolvedValue(makeDevice())
    mockedFetchMetrics.mockResolvedValue([])

    const { unmount } = render(
      <DeviceDetail device={makeDevice()} onBack={() => {}} />,
    )
    await flushInitialPoll()
    expect(mockedFetchDevice).toHaveBeenCalledTimes(1)

    unmount()
    await act(async () => {
      await vi.advanceTimersByTime(20000)
    })

    expect(mockedFetchDevice).toHaveBeenCalledTimes(1)
  })

  it('shows "Device not found" with a back button when the device request 404s', async () => {
    mockedFetchDevice.mockRejectedValue(new DeviceNotFoundError('uuid-1'))
    mockedFetchMetrics.mockResolvedValue([])
    const onBack = vi.fn()

    render(<DeviceDetail device={makeDevice()} onBack={onBack} />)
    await flushInitialPoll()

    expect(screen.getByText('Device not found')).toBeTruthy()
    fireEvent.click(
      screen.getByRole('button', { name: /back to devices/i }),
    )
    expect(onBack).toHaveBeenCalledTimes(1)
  })

  it('remounts on a key change so a different device record shows immediately', async () => {
    mockedFetchDevice.mockImplementation(() => new Promise(() => {}))
    mockedFetchMetrics.mockResolvedValue([])

    const alpha = makeDevice({ uuid: 'uuid-a', hostname: 'alpha' })
    const bravo = makeDevice({ uuid: 'uuid-b', hostname: 'bravo' })

    function Harness({ device }: { device: Device }) {
      return <DeviceDetail key={device.uuid} device={device} onBack={() => {}} />
    }

    const { rerender } = render(<Harness device={alpha} />)
    expect(screen.getByRole('heading', { name: 'alpha' })).toBeTruthy()

    rerender(<Harness device={bravo} />)

    expect(screen.getByRole('heading', { name: 'bravo' })).toBeTruthy()
    expect(screen.queryByRole('heading', { name: 'alpha' })).toBeNull()
  })
})
