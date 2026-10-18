# Sentinel Dashboard

A web dashboard for [Sentinel](../../README.md). It shows every device
registered with the Sentinel API, its live ONLINE/OFFLINE status, and a detail
view with recent metrics (CPU, memory, disk, uptime, and network throughput).

## Stack

- React 19 + TypeScript (strict)
- Vite 8 (dev server and build)
- Tailwind CSS 4
- Recharts 3 (metric charts)
- Vitest (unit tests for the value formatters)

## Requirements

- Node.js 20 or newer (npm is included with Node)
- The Sentinel API running on `http://localhost:8080`

## Getting started

```bash
npm install
npm run dev
```

The dev server starts on `http://localhost:5173`. In development the app calls
relative `/api/...` URLs and Vite proxies them to the Sentinel API
(`http://localhost:8080` by default), so there is no CORS configuration
anywhere. Point the proxy at a different API with `VITE_API_PROXY_TARGET`:

```bash
VITE_API_PROXY_TARGET=http://192.168.1.50:8080 npm run dev
```

## Scripts

| Command            | Description                                          |
| ------------------ | ---------------------------------------------------- |
| `npm run dev`      | Start the Vite dev server with HMR                    |
| `npm run build`    | Type-check with `tsc` and build for production        |
| `npm test`         | Run the Vitest unit tests                             |
| `npm run lint`     | Run oxlint                                            |
| `npm run preview`  | Serve the production build locally                    |

## Endpoints used

- `POST /api/v1/auth/login` – exchange username and password for a JWT
- `GET /api/v1/devices` – the list of devices (polled every 10 seconds)
- `GET /api/v1/devices/{uuid}/metrics` – the last 100 readings for a device
  (polled every 5 seconds while a device is open)
- `POST /api/v1/devices/{uuid}/revoke-token` – revoke a device's token
  (invoked from the device detail view)

Every request after login sends `Authorization: Bearer <jwt>`. The token is kept
in `sessionStorage` (so it is cleared when the tab closes) and in memory. A
`401` response clears the token and returns to the login screen.

Polling pauses automatically when the browser tab is hidden, and no request is
started while a previous one is still in flight.