import { describe, expect, it } from 'vitest'
import {
  EMPTY_VALUE,
  computePercent,
  formatBytes,
  formatRate,
  formatRelative,
  formatUptime,
} from './format'

describe('formatBytes', () => {
  it('returns the placeholder for missing values', () => {
    expect(formatBytes(null)).toBe(EMPTY_VALUE)
    expect(formatBytes(undefined)).toBe(EMPTY_VALUE)
    expect(formatBytes(Number.NaN)).toBe(EMPTY_VALUE)
  })

  it('formats byte-sized values', () => {
    expect(formatBytes(0)).toBe('0 B')
    expect(formatBytes(512)).toBe('512 B')
    expect(formatBytes(1023)).toBe('1023 B')
  })

  it('switches to KB at 1024 and above', () => {
    expect(formatBytes(1024)).toBe('1 KB')
    expect(formatBytes(1536)).toBe('1.5 KB')
    expect(formatBytes(1024 * 1024 - 1)).toBe('1024 KB')
  })

  it('switches to MB at the next boundary', () => {
    expect(formatBytes(1024 * 1024)).toBe('1 MB')
    expect(formatBytes(2.5 * 1024 * 1024)).toBe('2.5 MB')
    expect(formatBytes(1024 ** 3 - 1)).toBe('1024 MB')
  })

  it('switches to GB for huge values', () => {
    expect(formatBytes(1024 ** 3)).toBe('1 GB')
    expect(formatBytes(3 * 1024 ** 3)).toBe('3 GB')
    expect(formatBytes(1024 ** 4)).toBe('1024 GB')
  })
})

describe('formatRate', () => {
  it('returns the placeholder when the value is missing', () => {
    expect(formatRate(null)).toBe(EMPTY_VALUE)
  })

  it('appends /s to the byte value', () => {
    expect(formatRate(1024)).toBe('1 KB/s')
    expect(formatRate(2048)).toBe('2 KB/s')
    expect(formatRate(1024 * 1024)).toBe('1 MB/s')
  })
})

describe('computePercent', () => {
  it('returns null when either value is missing', () => {
    expect(computePercent(null, 100)).toBeNull()
    expect(computePercent(50, null)).toBeNull()
    expect(computePercent(undefined, 100)).toBeNull()
  })

  it('returns null when the total is not positive', () => {
    expect(computePercent(10, 0)).toBeNull()
    expect(computePercent(10, -5)).toBeNull()
  })

  it('computes the percentage', () => {
    expect(computePercent(50, 100)).toBe(50)
    expect(computePercent(10, 200)).toBe(5)
    expect(computePercent(128, 1024)).toBeCloseTo(12.5)
  })
})

describe('formatUptime', () => {
  it('returns the placeholder for missing or invalid values', () => {
    expect(formatUptime(null)).toBe(EMPTY_VALUE)
    expect(formatUptime(undefined)).toBe(EMPTY_VALUE)
    expect(formatUptime(Number.NaN)).toBe(EMPTY_VALUE)
    expect(formatUptime(-10)).toBe(EMPTY_VALUE)
  })

  it('formats seconds, minutes, hours and days', () => {
    expect(formatUptime(0)).toBe('0s')
    expect(formatUptime(45)).toBe('45s')
    expect(formatUptime(90)).toBe('1m 30s')
    expect(formatUptime(3600)).toBe('1h 0m')
    expect(formatUptime(3661)).toBe('1h 1m')
    expect(formatUptime(90061)).toBe('1d 1h 1m')
  })
})

describe('formatRelative', () => {
  const now = new Date('2026-10-09T12:00:00Z')

  it('returns the placeholder for missing or malformed timestamps', () => {
    expect(formatRelative(null, now)).toBe(EMPTY_VALUE)
    expect(formatRelative(undefined, now)).toBe(EMPTY_VALUE)
    expect(formatRelative('not-a-date', now)).toBe(EMPTY_VALUE)
  })

  it('labels very recent and future timestamps as now', () => {
    expect(formatRelative('2026-10-09T11:59:55Z', now)).toBe('just now')
    expect(formatRelative('2026-10-09T12:00:05Z', now)).toBe('just now')
  })

  it('formats minutes, hours and days ago', () => {
    expect(formatRelative('2026-10-09T11:59:15Z', now)).toBe('45s ago')
    expect(formatRelative('2026-10-09T11:57:00Z', now)).toBe('3m ago')
    expect(formatRelative('2026-10-09T10:00:00Z', now)).toBe('2h ago')
    expect(formatRelative('2026-10-08T06:00:00Z', now)).toBe('1d ago')
    expect(formatRelative('2026-10-03T12:00:00Z', now)).toBe('6d ago')
  })

  it('falls back to an absolute date after a week', () => {
    expect(formatRelative('2026-09-29T12:00:00Z', now)).toBe(
      new Date('2026-09-29T12:00:00Z').toLocaleString(),
    )
  })
})