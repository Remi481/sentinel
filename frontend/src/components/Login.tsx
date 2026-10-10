import { useState } from 'react'
import { InvalidCredentialsError, login } from '../api'
import { setToken } from '../auth'

interface LoginProps {
  onLoggedIn: () => void
  notice?: string | null
}

export default function Login({ onLoggedIn, notice }: LoginProps) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (busy) return
    setBusy(true)
    setError(null)
    try {
      const token = await login(username, password)
      setToken(token)
      onLoggedIn()
    } catch (err) {
      if (err instanceof InvalidCredentialsError) {
        setError('Invalid username or password')
      } else {
        setError('Cannot reach the Sentinel API')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-neutral-50 px-4 text-neutral-900 dark:bg-neutral-950 dark:text-neutral-100">
      <form
        onSubmit={onSubmit}
        className="w-full max-w-sm rounded-xl border border-neutral-200 bg-white p-6 shadow-sm dark:border-neutral-800 dark:bg-neutral-900"
      >
        <h1 className="text-xl font-bold tracking-tight">Sentinel</h1>
        <p className="mt-1 text-sm text-neutral-500 dark:text-neutral-400">
          Sign in to view your devices.
        </p>

        {notice ? (
          <div className="mt-4 rounded-lg border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-800 dark:border-amber-800 dark:bg-amber-950/40 dark:text-amber-200">
            {notice}
          </div>
        ) : null}

        <label className="mt-4 block text-sm font-medium" htmlFor="username">
          Username
        </label>
        <input
          id="username"
          name="username"
          autoComplete="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          className="mt-1 w-full rounded-lg border border-neutral-300 bg-white px-3 py-2 text-sm outline-none focus:border-sky-500 dark:border-neutral-700 dark:bg-neutral-950"
        />

        <label className="mt-4 block text-sm font-medium" htmlFor="password">
          Password
        </label>
        <input
          id="password"
          name="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="mt-1 w-full rounded-lg border border-neutral-300 bg-white px-3 py-2 text-sm outline-none focus:border-sky-500 dark:border-neutral-700 dark:bg-neutral-950"
        />

        {error ? (
          <p
            role="alert"
            className="mt-3 text-sm text-rose-600 dark:text-rose-400"
          >
            {error}
          </p>
        ) : null}

        <button
          type="submit"
          disabled={busy}
          className="mt-5 w-full rounded-lg bg-sky-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-sky-500 disabled:opacity-60"
        >
          {busy ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
    </div>
  )
}
