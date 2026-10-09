# Sentinel API Specification

The Sentinel backend is a Spring Boot 4 application. This document describes the HTTP API as it is implemented today (version `0.1.0`).

- **Base URL:** all endpoints are prefixed with `/api/v1` (backend runs on `http://localhost:8080` by default).
- **Payloads:** request and response bodies are JSON (`application/json`).
- **Timestamps:** written as ISO-8601 instants in UTC, e.g. `2026-10-09T10:46:27.386331Z`, with variable fractional-second precision.
- **Authentication:** global security is configured (`anyRequest().authenticated()`), but today only the `/api/v1/devices/**` and `/api/v1/metrics` paths are open. See [Known gaps](#known-gaps).

## Endpoints

| Method | Path | Description | Status |
| --- | --- | --- | --- |
| GET | `/api/v1/health` | Service liveness info | Implemented (see known gaps) |
| POST | `/api/v1/devices/register` | Register or update an agent | Implemented |
| POST | `/api/v1/devices/heartbeat` | Report that an agent is alive | Implemented |
| GET | `/api/v1/devices` | List all registered devices | Implemented |
| GET | `/api/v1/devices/{uuid}` | Fetch one device | Implemented |
| POST | `/api/v1/metrics` | Store one metrics sample | Implemented |
| GET | `/api/v1/devices/{uuid}/metrics` | Fetch recent metrics history | Implemented |

## Health

### GET `/api/v1/health`

Returns static service information.

Example response (200):

```json
{
  "status": "UP",
  "service": "Sentinel API",
  "version": "0.1.0"
}
```

- `status`: always `UP` at present; the endpoint is a static check and does not reflect database connectivity.

## Devices

The `Device` JSON shape is:

| Field | Type | Notes |
| --- | --- | --- |
| `uuid` | string (UUID) | Client-generated; primary key. Required. |
| `hostname` | string | nullable |
| `operatingSystem` | string | nullable |
| `architecture` | string | nullable |
| `ipAddress` | string | nullable |
| `agentVersion` | string | nullable |
| `status` | enum `ONLINE` / `OFFLINE` / `UNKNOWN` | Ignored on input; see below. |
| `lastSeen` | instant | Ignored on input; see below. |

On `register` and `heartbeat` the server sets `status = ONLINE` and `lastSeen = now`, so any `status`/`lastSeen` sent by the client are overridden.

### POST `/api/v1/devices/register`

Registers an agent device. If a device with the same `uuid` already exists it is updated; otherwise it is created. Both new and existing devices are marked `ONLINE`.

Example request:

```json
{
  "uuid": "9b2f8c01-9a1e-4b7a-8d5e-1a2b3c4d5e6f",
  "hostname": "agent-01",
  "operatingSystem": "Debian 12",
  "architecture": "aarch64",
  "ipAddress": "192.168.1.50",
  "agentVersion": "0.1.0"
}
```

Example response (200 OK) with the saved device:

```json
{
  "uuid": "9b2f8c01-9a1e-4b7a-8d5e-1a2b3c4d5e6f",
  "hostname": "agent-01",
  "operatingSystem": "Debian 12",
  "architecture": "aarch64",
  "ipAddress": "192.168.1.50",
  "agentVersion": "0.1.0",
  "status": "ONLINE",
  "lastSeen": "2026-10-09T10:46:27.039415108Z"
}
```

### POST `/api/v1/devices/heartbeat`

Marks a device as alive (`ONLINE`, `lastSeen = now`). Body is a `HeartbeatRequest` containing only the device uuid.

Example request:

```json
{
  "uuid": "9b2f8c01-9a1e-4b7a-8d5e-1a2b3c4d5e6f"
}
```

- `200 OK`: device updated; the full device JSON is returned.
- `404 Not Found`: plain-text body `Device not found: {uuid}`.

### GET `/api/v1/devices`

Lists all registered devices. Returns a JSON array of device objects. No ordering is guaranteed.

Example response (200 OK):

```json
[
  {
    "uuid": "9b2f8c01-9a1e-4b7a-8d5e-1a2b3c4d5e6f",
    "hostname": "agent-01",
    "operatingSystem": "Debian 12",
    "architecture": "aarch64",
    "ipAddress": "192.168.1.50",
    "agentVersion": "0.1.0",
    "status": "ONLINE",
    "lastSeen": "2026-10-09T10:46:27.187471566Z"
  }
]
```

### GET `/api/v1/devices/{uuid}`

Fetches a single device.

- `200 OK`: the device JSON.
- `404 Not Found`: plain-text body `Device not found: {uuid}`.

## Metrics

The `MetricsRequest` JSON shape:

| Field | Type | Notes |
| --- | --- | --- |
| `uuid` | string (UUID) | Device uuid. Required. |
| `cpuUsagePercent` | number | nullable |
| `memoryUsedBytes` | integer | nullable |
| `memoryTotalBytes` | integer | nullable |
| `diskUsedBytes` | integer | nullable |
| `diskTotalBytes` | integer | nullable |
| `uptimeSeconds` | integer | nullable |
| `networkReceivedBytesPerSecond` | integer | nullable |
| `networkSentBytesPerSecond` | integer | nullable |

### POST `/api/v1/metrics`

Stores one metrics sample for a device.

- `201 Created`: empty body on success.
- `404 Not Found`: plain-text body `Device not found: {uuid}` when the device is unknown.

### GET `/api/v1/devices/{uuid}/metrics`

Returns the most recent metrics samples for a device, newest first (most recent 100).

Each sample is a `MetricsResponse`:

| Field | Type | Notes |
| --- | --- | --- |
| `recordedAt` | instant | When the sample was stored |
| `cpuUsagePercent` | number | nullable |
| `memoryUsedBytes` | integer | nullable |
| `memoryTotalBytes` | integer | nullable |
| `diskUsedBytes` | integer | nullable |
| `diskTotalBytes` | integer | nullable |
| `uptimeSeconds` | integer | nullable |
| `networkReceivedBytesPerSecond` | integer | nullable |
| `networkSentBytesPerSecond` | integer | nullable |

Example response (200 OK):

```json
[
  {
    "recordedAt": "2026-10-09T10:46:27.386331Z",
    "cpuUsagePercent": 2.5,
    "memoryUsedBytes": 22020096,
    "memoryTotalBytes": 83886080,
    "diskUsedBytes": 1073741824,
    "diskTotalBytes": 21474836480,
    "uptimeSeconds": 250,
    "networkReceivedBytesPerSecond": 512,
    "networkSentBytesPerSecond": 4096
  }
]
```

- `404 Not Found`: plain-text body `Device not found: {uuid}` when the device is unknown.

## Error handling

- Unknown devices on reads, heartbeats, and metric reports produce `404 Not Found` with a plain-text body `Device not found: {uuid}`.
- Other error responses (e.g. malformed JSON or internal errors) are not customized yet and use Spring's default behavior.

## Offline detection

A scheduled task periodically flips devices to `OFFLINE`:

- a device becomes `OFFLINE` when `lastSeen` is older than 90 seconds (or `lastSeen` is null),
- the check runs every 30 seconds.

The agent is expected to call `register` or `heartbeat` at a shorter interval (default 30 s) so it stays `ONLINE`.

## Known gaps

- `GET /api/v1/health` is not in the `permitAll` list, and no authentication mechanism (HTTP Basic or form login) is configured. In practice the endpoint returns `403 Forbidden` to all callers until security is wired up or the path is permitted.
- Request bodies are not validated (`jakarta.validation` is on the classpath but no constraints are enforced yet).
- There is no pagination; the metrics history is fixed at 100 samples.

## Not built yet

The endpoints below were planned in an earlier draft and do not exist yet. They are listed here so the intent is not lost; nothing under them should be relied upon.

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/auth/login` | Authenticate a user and return a JWT |
| GET | `/api/devices` | (pre-`/api/v1` layout) |
| GET | `/api/devices/{id}` | (pre-`/api/v1` layout) |
| GET | `/api/metrics/{deviceId}` | (pre-`/api/v1` layout) |
| GET | `/api/alerts` | Retrieve active alerts |
| POST | `/api/alerts` | Create an alert |
| PATCH | `/api/alerts/{id}` | Mark an alert as resolved |
| GET | `/api/health` | (pre-`/api/v1` layout) |

- Device audit/history and alerting are not implemented.