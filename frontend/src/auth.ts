const TOKEN_KEY = 'sentinel.auth.token'

let inMemoryToken: string | null = null
let sessionExpiredHandler: (() => void) | null = null

export function getToken(): string | null {
  if (inMemoryToken !== null) {
    return inMemoryToken
  }
  try {
    const stored = window.sessionStorage.getItem(TOKEN_KEY)
    inMemoryToken = stored
    return stored
  } catch {
    return null
  }
}

export function setToken(token: string): void {
  inMemoryToken = token
  try {
    window.sessionStorage.setItem(TOKEN_KEY, token)
  } catch {
    // sessionStorage unavailable: the in-memory token still works for this page.
  }
}

export function clearToken(): void {
  inMemoryToken = null
  try {
    window.sessionStorage.removeItem(TOKEN_KEY)
  } catch {
    // Nothing to clear.
  }
}

export function isLoggedIn(): boolean {
  return getToken() !== null
}

export function onSessionExpired(handler: () => void): () => void {
  sessionExpiredHandler = handler
  return () => {
    if (sessionExpiredHandler === handler) {
      sessionExpiredHandler = null
    }
  }
}

export function expireSession(): void {
  clearToken()
  if (sessionExpiredHandler) {
    sessionExpiredHandler()
  }
}
