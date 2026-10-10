import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  InvalidCredentialsError,
  UnauthorizedError,
  fetchDevices,
  login,
  revokeDeviceToken,
} from './api'
import { getToken, onSessionExpired, setToken } from './auth'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

beforeEach(() => {
  setToken('test-jwt')
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('authenticated requests', () => {
  it('attaches the bearer token', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([]))
    vi.stubGlobal('fetch', fetchMock)

    await fetchDevices()

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/v1/devices')
    expect(init.headers).toMatchObject({ Authorization: 'Bearer test-jwt' })
  })

  it('clears the token and notifies on 401', async () => {
    const handler = vi.fn()
    const unsubscribe = onSessionExpired(handler)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({}, 401)))

    await expect(fetchDevices()).rejects.toBeInstanceOf(UnauthorizedError)

    expect(handler).toHaveBeenCalledTimes(1)
    expect(getToken()).toBeNull()
    unsubscribe()
  })

  it('sends a revoke request with the bearer token', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)

    await revokeDeviceToken('9b2f8c01-9a1e-4b7a-8d5e-1a2b3c4d5e6f')

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe(
      '/api/v1/devices/9b2f8c01-9a1e-4b7a-8d5e-1a2b3c4d5e6f/revoke-token',
    )
    expect(init.method).toBe('POST')
    expect(init.headers).toMatchObject({ Authorization: 'Bearer test-jwt' })
  })
})

describe('login', () => {
  it('posts credentials and returns the token', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        jsonResponse({ token: 'issued-jwt', expiresInSeconds: 28800 }),
      )
    vi.stubGlobal('fetch', fetchMock)

    await expect(login('admin', 'secret')).resolves.toBe('issued-jwt')

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/v1/auth/login')
    expect(init.method).toBe('POST')
    expect(init.body).toBe(
      JSON.stringify({ username: 'admin', password: 'secret' }),
    )
  })

  it('maps a 401 to InvalidCredentialsError', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({}, 401)))

    await expect(login('admin', 'wrong')).rejects.toBeInstanceOf(
      InvalidCredentialsError,
    )
  })
})
