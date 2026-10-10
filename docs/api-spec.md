# Sentinel API Specification

The Sentinel backend is a Spring Boot 4 application. This document describes the HTTP API as it is implemented today (version `0.1.0`).

- **Base URL:** all endpoints are prefixed with `/api/v1` (backend runs on `http://localhost:8080` by default).
- **Payloads:** request and response bodies are JSON (`application/json`).
- **Timestamps:** written as ISO-8601 instants in UTC, e.g. `2026-10-09T10:46:27.386331Z`, with variable fractional-second precision.
- **Authentication:** dashboard and admin endpoints require a JWT from `POST /api/v1/auth/login`. Agent endpoints authenticate with a per-device token (or the enrolment key on first registration). See [Authentication](#authentication).

## Endpoints

| Method | Path | Description | Auth |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/login` | Log in and receive a JWT | none |
| GET | `/api/v1/health` | Service liveness info | none |
| POST | `/api/v1/devices/register` | Register or update an agent | enrolment key, or device token |
| POST | `/api/v1/devices/heartbeat` | Report that an agent is alive | device token |
| POST | `/api/v1/metrics` | Store one metrics sample | device token |
| GET | `/api/v1/devices` | List all registered devices | JWT (ADMIN) |
| GET | `/api/v1/devices/{uuid}` | Fetch one device | JWT (ADMIN) |
| GET | `/api/v1/devices/{uuid}/metrics` | Fetch recent metrics history | JWT (ADMIN) |
| POST | `/api/v1/devices/{uuid}/revoke-token` | Revoke a device's token | JWT (ADMIN) |

## Authentication

There are two authentication schemes. The full design and operational guidance
live in [security.md](security.md); this section is the wire-level contract.

### Dashboard JWT

- Send `Authorization: Bearer <jwt>` on protected requests.
- A missing, malformed, expired, or wrongly-signed JWT is rejected with `401`.
- A valid JWT whose `role` is not `ADMIN` is rejected with `403`.
- Tokens are signed HS256 and are stateless; there is no session cookie.

### Device token

- After the first registration each device has an opaque token.
- Send it as `Authorization: Bearer <token>` on `register` (update form),
  `heartbeat`, and `metrics`.
- The token must belong to the device `uuid` in the request; a token for a
  different device is rejected with `403`. An unknown or revoked token is
  rejected with `401`.
- Because device tokens are opaque (not JWTs), the API distinguishes them from
  dashboard JWTs before validation. This means `401`/`403` from an agent
  endpoint always refers to the device token, not a dashboard login.

### Enrolment key

- The first registration for a device authenticates with the `X-Enrollment-Key`
  header instead of a token.
- A missing or wrong key is rejected with `401`.

## Login

### POST `/api/v1/auth/login`

Exchanges a username and password for a JWT.

Example request:

```json
{
  "username": "admin",
  "password": "correct horse battery staple"
}
```

Example response (200 OK):

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresInSeconds": 28800
}
```

- `401 Unauthorized`: username or password is wrong.
- `400 Bad Request`: `username` or `password` is missing or blank. Both fields
  are required (`@NotBlank`).

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

On `register` and `heartbeat` the server sets `status = ONLINE` and `lastSeen = now`, so any `status`/`lastSeen` sent by the client are overridden. The response never includes the device token or its hash.

### POST `/api/v1/devices/register`

Registers an agent device. If a device with the same `uuid` already exists it is updated; otherwise it is created. Both new and existing devices are marked `ONLINE`.

This endpoint has two forms, selected automatically based on whether the device
is already enrolled (has a token):

- **First registration (enrolment).** The device has no token yet. Authenticate
  with the `X-Enrollment-Key` header. The API creates the device and returns a
  freshly generated `deviceToken`.
- **Update (`re-registration`).** The device already has a token. Send
  `Authorization: Bearer <deviceToken>`. The API updates the device metadata and
  returns `deviceToken: null`. The token is never rotated here.

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

Example response (200 OK) for a first registration:

```json
{
  "device": {
    "uuid": "9b2f8c01-9a1e-4b7a-8d5e-1a2b3c4d5e6f",
    "hostname": "agent-01",
    "operatingSystem": "Debian 12",
    "architecture": "aarch64",
    "ipAddress": "192.168.1.50",
    "agentVersion": "0.1.0",
    "status": "ONLINE",
    "lastSeen": "2026-10-09T10:46:27.039415108Z"
  },
  "deviceToken": "e7f1c2a9b4d6f0e3..."
}
```

On an update (device token supplied) the response is the same shape with
`"deviceToken": null`.

- `401 Unauthorized`: first registration without a valid `X-Enrollment-Key`, or
  an update with a missing/invalid device token.
- `403 Forbidden`: the device token does not belong to the submitted `uuid`.

### POST `/api/v1/devices/heartbeat`

Marks a device as alive (`ONLINE`, `lastSeen = now`). Body is a `HeartbeatRequest` containing only the device uuid. Requires a device token (`Authorization: Bearer <token>`) that belongs to the given uuid.

Example request:

```json
{
  "uuid": "9b2f8c01-9a1e-4b7a-8d5e-1a2b3c4d5e6f"
}
```

- `200 OK` — device updated; the full device JSON is returned.
- `401 Unauthorized`: the device token is missing, unknown, or revoked.
- `403 Forbidden`: the device token belongs to a different device.

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

- `200 OK` — the device JSON.
- `404 Not Found` — plain-text body `Device not found: {uuid}`.

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

Stores one metrics sample for a device. Requires a device token (`Authorization: Bearer <token>`) that belongs to the `uuid` in the body.

- `201 Created` — empty body on success.
- `401 Unauthorized`: missing, unknown, or revoked device token.
- `403 Forbidden`: the device token does not belong to the submitted `uuid`.

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

- `404 Not Found` — plain-text body `Device not found: {uuid}` when the device is unknown.

## Token revocation

### POST `/api/v1/devices/{uuid}/revoke-token`

Revokes a device's token. Requires a dashboard JWT. Clears the stored token hash
so the device's current token immediately stops working. The device row and its
metric history are kept.

- `204 No Content`: the token was revoked (or the device had none).
- `401 Unauthorized`: missing or invalid JWT.
- `403 Forbidden`: a valid JWT without the `ADMIN` role.
- `404 Not Found`: plain-text body `Device not found: {uuid}` when the device is unknown.

After revocation the agent's next call returns `401`. Delete the agent's
`~/.sentinel-agent/device-token` file and restart it to re-enrol. See
[security.md](security.md#revocation).

## Error handling

- Missing or invalid credentials produce `401 Unauthorized`; a valid credential
  without the required authority produces `403 Forbidden`.
- Dashboard endpoints (`GET /api/v1/devices`, `GET /api/v1/devices/{uuid}`,
  `GET /api/v1/devices/{uuid}/metrics`, and `POST .../revoke-token`) require an
  `ADMIN` JWT.
- Unknown devices on the authenticated dashboard reads produce `404 Not Found`
  with a plain-text body `Device not found: {uuid}`. Because agent endpoints
  authenticate before touching the device, an unknown or revoked token there
  yields `401` rather than `404`.
- Malformed login bodies produce `400 Bad Request` (see [Login](#login)).
  Other malformed JSON or internal errors are not customized yet and use
  Spring's default behavior.

## Offline detection

A scheduled task periodically flips devices to `OFFLINE`:

- a device becomes `OFFLINE` when `lastSeen` is older than 90 seconds (or `lastSeen` is null),
- the check runs every 30 seconds.

The agent is expected to call `register` or `heartbeat` at a shorter interval (default 30 s) so it stays `ONLINE`.

## Known gaps

- Request bodies other than login are not validated with `jakarta.validation`.
- There is no pagination; the metrics history is fixed at 100 samples.

## Not built yet

The endpoints below were planned in an earlier draft and do not exist yet. They are listed here so the intent is not lost; nothing under them should be relied upon.

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/devices` | (pre-`/api/v1` layout) |
| GET | `/api/devices/{id}` | (pre-`/api/v1` layout) |
| GET | `/api/metrics/{deviceId}` | (pre-`/api/v1` layout) |
| GET | `/api/alerts` | Retrieve active alerts |
| POST | `/api/alerts` | Create an alert |
| PATCH | `/api/alerts/{id}` | Mark an alert as resolved |
| GET | `/api/health` | (pre-`/api/v1` layout) |

- Device audit/history and alerting are not implemented.