import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  clearToken,
  expireSession,
  getToken,
  isLoggedIn,
  onSessionExpired,
  setToken,
} from './auth'

function createStorage(): Storage {
  const map = new Map<string, string>()
  return {
    get length() {
      return map.size
    },
    clear: () => map.clear(),
    getItem: (key: string) => (map.has(key) ? map.get(key)! : null),
    key: (index: number) => Array.from(map.keys())[index] ?? null,
    removeItem: (key: string) => {
      map.delete(key)
    },
    setItem: (key: string, value: string) => {
      map.set(key, value)
    },
  } as Storage
}

let storage: Storage

beforeEach(() => {
  storage = createStorage()
  vi.stubGlobal('window', { sessionStorage: storage })
  clearToken()
})

describe('token storage', () => {
  it('starts logged out', () => {
    expect(getToken()).toBeNull()
    expect(isLoggedIn()).toBe(false)
  })

  it('round-trips a token through memory and sessionStorage', () => {
    setToken('jwt-value')

    expect(getToken()).toBe('jwt-value')
    expect(isLoggedIn()).toBe(true)
    expect(storage.getItem('sentinel.auth.token')).toBe('jwt-value')
  })

  it('clears the token from both stores', () => {
    setToken('jwt-value')
    clearToken()

    expect(getToken()).toBeNull()
    expect(storage.getItem('sentinel.auth.token')).toBeNull()
  })

  it('re-reads the token from sessionStorage when memory is empty', () => {
    storage.setItem('sentinel.auth.token', 'persisted')
    expect(getToken()).toBe('persisted')
  })

  it('tolerates an unavailable sessionStorage', () => {
    vi.stubGlobal('window', undefined)
    expect(() => setToken('jwt-value')).not.toThrow()
    expect(getToken()).toBe('jwt-value')
  })
})

describe('session expiry', () => {
  it('clears the token and notifies the handler', () => {
    const handler = vi.fn()
    const unsubscribe = onSessionExpired(handler)
    setToken('jwt-value')

    expireSession()

    expect(handler).toHaveBeenCalledTimes(1)
    expect(getToken()).toBeNull()
    unsubscribe()
  })

  it('stops notifying after unsubscribe', () => {
    const handler = vi.fn()
    onSessionExpired(handler)()

    expireSession()

    expect(handler).not.toHaveBeenCalled()
  })
})
