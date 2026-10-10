import { useEffect, useMemo, useState } from 'react'
import { fetchDevices } from './api'
import { clearToken, isLoggedIn, onSessionExpired } from './auth'
import type { Device } from './types'
import DeviceDetail from './components/DeviceDetail'
import DeviceList from './components/DeviceList'
import Login from './components/Login'

const DEVICES_POLL_MS = 10000
const NOW_TICK_MS = 30000

function App() {
  const [authed, setAuthed] = useState(() => isLoggedIn())
  const [notice, setNotice] = useState<string | null>(null)
  const [devices, setDevices] = useState<Device[] | null>(null)
  const [error, setError] = useState(false)
  const [selected, setSelected] = useState<Device | null>(null)
  const [now, setNow] = useState(() => new Date())

  useEffect(
    () =>
      onSessionExpired(() => {
        setAuthed(false)
        setNotice('Your session has expired. Please log in again.')
        setDevices(null)
        setError(false)
        setSelected(null)
      }),
    [],
  )

  function onLoggedIn() {
    setNotice(null)
    setDevices(null)
    setAuthed(true)
  }

  function onLogout() {
    clearToken()
    setNotice(null)
    setDevices(null)
    setError(false)
    setSelected(null)
    setAuthed(false)
  }

  useEffect(() => {
    if (!authed) return

    let cancelled = false
    let timeout: number | undefined
    let inFlight = false

    async function poll() {
      if (cancelled || inFlight) return
      inFlight = true
      try {
        const data = await fetchDevices()
        if (cancelled) return
        setDevices(data)
        setError(false)
      } catch {
        if (cancelled) return
        setError(true)
      } finally {
        inFlight = false
        if (!cancelled) schedule(DEVICES_POLL_MS)
      }
    }

    function schedule(delay: number) {
      if (document.hidden) return
      timeout = window.setTimeout(() => {
        timeout = undefined
        void poll()
      }, delay)
    }

    function onVisibility() {
      if (!document.hidden && timeout == null) schedule(0)
    }

    void poll()
    document.addEventListener('visibilitychange', onVisibility)
    return () => {
      cancelled = true
      document.removeEventListener('visibilitychange', onVisibility)
      window.clearTimeout(timeout)
    }
  }, [authed])

  useEffect(() => {
    let interval: number | undefined

    function tick() {
      setNow(new Date())
    }

    function onVisibility() {
      if (!document.hidden) {
        tick()
        if (interval == null) interval = window.setInterval(tick, NOW_TICK_MS)
      } else if (interval != null) {
        window.clearInterval(interval)
        interval = undefined
      }
    }

    interval = window.setInterval(tick, NOW_TICK_MS)
    document.addEventListener('visibilitychange', onVisibility)
    return () => {
      window.clearInterval(interval)
      document.removeEventListener('visibilitychange', onVisibility)
    }
  }, [])

  const sortedDevices = useMemo(() => {
    if (!devices) return []
    return [...devices].sort((a, b) => {
      if (a.status !== b.status) return a.status === 'ONLINE' ? -1 : 1
      return a.hostname.localeCompare(b.hostname, undefined, {
        sensitivity: 'base',
      })
    })
  }, [devices])

  const onlineCount = devices?.filter((d) => d.status === 'ONLINE').length ?? 0

  if (!authed) {
    return <Login onLoggedIn={onLoggedIn} notice={notice} />
  }

  return (
    <div className="min-h-screen bg-neutral-50 text-neutral-900 dark:bg-neutral-950 dark:text-neutral-100">
      <header className="mx-auto w-full max-w-6xl px-4 pb-2 pt-8">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h1 className="text-xl font-bold tracking-tight">Sentinel</h1>
          <div className="flex items-center gap-3">
            <p className="text-sm text-neutral-500 dark:text-neutral-400">
              {onlineCount} of {devices?.length ?? 0} devices online
            </p>
            <button
              type="button"
              onClick={onLogout}
              className="rounded-lg border border-neutral-300 px-3 py-1 text-sm font-medium text-neutral-600 transition-colors hover:bg-neutral-100 dark:border-neutral-700 dark:text-neutral-300 dark:hover:bg-neutral-800"
            >
              Log out
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl px-4 py-6">
        {selected ? (
          <DeviceDetail device={selected} onBack={() => setSelected(null)} />
        ) : error ? (
          <div className="mb-4 rounded-lg border border-amber-300 bg-amber-50 px-4 py-2 text-sm text-amber-800 dark:border-amber-800 dark:bg-amber-950/40 dark:text-amber-200">
            Cannot reach the Sentinel API
          </div>
        ) : null}

        {!selected &&
          (devices === null ? (
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {Array.from({ length: 6 }, (_, i) => (
                <div
                  key={i}
                  className="h-40 animate-pulse rounded-xl bg-neutral-200 dark:bg-neutral-800"
                />
              ))}
            </div>
          ) : devices.length === 0 ? (
            <div className="rounded-xl border border-dashed border-neutral-300 p-10 text-center text-sm text-neutral-500 dark:border-neutral-700 dark:text-neutral-400">
              No devices registered yet. Start the Sentinel agent on a device and
              it will appear here automatically.
            </div>
          ) : (
            <DeviceList
              devices={sortedDevices}
              now={now}
              onSelect={setSelected}
            />
          ))}
      </main>
    </div>
  )
}

export default App