# Sentinel Security

Sentinel has two distinct callers, and each uses a different kind of
credential:

| Caller | Credential | Lifetime | Stored by the caller | Stored by the API |
| --- | --- | --- | --- | --- |
| Dashboard user | JWT (HS256) | 8 hours by default | In `sessionStorage` and memory | Nothing (stateless) |
| Monitoring agent | Opaque device token | Until revoked | `~/.sentinel-agent/device-token` | SHA-256 hash only |

The API itself never stores a user password in plain text and never stores an
agent token in plain text.

## Configuration

All security settings come from environment variables. Startup fails fast if
they are missing or too weak.

| Variable | Required | Notes |
| --- | --- | --- |
| `SENTINEL_JWT_SECRET` | yes | Signing key for dashboard JWTs. At least 32 bytes of random data. Generate with `openssl rand -base64 32`. |
| `SENTINEL_JWT_TTL_SECONDS` | no | JWT lifetime in seconds. Defaults to `28800` (8 hours). |
| `SENTINEL_ENROLLMENT_KEY` | yes | Shared secret that a new agent presents once to enrol. At least 24 characters. |
| `SENTINEL_ADMIN_USERNAME` | together | Bootstrap admin username. Provide together with the password, or leave both unset. |
| `SENTINEL_ADMIN_PASSWORD` | together | Bootstrap admin password. At least 12 characters. |
| `SENTINEL_DB_PASSWORD` | prod only | Database password. Required when the `prod` profile is active, and must not be the insecure default `sentinel`. |

`SENTINEL_JWT_TTL_SECONDS` is an extra convenience setting added by this
implementation; it is not part of the original requirements.

### Startup validation

Startup checks all of the above at once and refuses to start if anything is
missing or too weak. It reports every problem in a single message (naming the
variables, never their values), so a misconfigured server fails immediately
rather than running with a weak secret. The checked conditions are:

- `SENTINEL_JWT_SECRET` is at least 32 bytes,
- `SENTINEL_ENROLLMENT_KEY` is at least 24 characters,
- `SENTINEL_ADMIN_USERNAME` and `SENTINEL_ADMIN_PASSWORD` are set together, and
  the password is at least 12 characters,
- with the `prod` profile active, `SENTINEL_DB_PASSWORD` is set and is not
  `sentinel`.


### Admin bootstrap

On startup, if the `app_users` table is empty and both `SENTINEL_ADMIN_USERNAME`
and `SENTINEL_ADMIN_PASSWORD` are set, the API creates a single `ADMIN` user
with a BCrypt-hashed password. If the table already has users, bootstrap is
skipped, so restarting the API never resets an existing password. An existing
admin's password is never overwritten.

If the admin variables are absent and the table is empty, the API logs a single
warning and continues; there will simply be nobody who can log in until an admin
is provisioned.

## Dashboard authentication

- `POST /api/v1/auth/login` with `{"username": "...", "password": "..."}`.
- The API verifies the password against the stored BCrypt hash. To avoid leaking
  which usernames exist through response timing, a failed lookup is compared
  against a dummy hash with the same cost.
- On success the API returns a signed JWT whose `role` claim is `ADMIN`
  (dashboard users are administrators).
- The dashboard keeps the JWT in `sessionStorage`, so it is cleared when the tab
  closes, and also in memory.
- Every protected request sends `Authorization: Bearer <jwt>`. A `401` clears
  the stored token and returns the user to the login screen with
  "Your session has expired. Please log in again."

### JWT validation

Tokens are signed with HS256 using `SENTINEL_JWT_SECRET`. The resource server is
stateless: no sessions or cookies are created. The `role` claim is mapped to a
`ROLE_ADMIN` authority, and the read and admin endpoints require it.

### Why a custom token resolver

Spring Security's default bearer token resolver forwards *any*
`Authorization: Bearer <value>` header to the JWT decoder. Agents also send
`Authorization: Bearer <opaque-token>`, and an opaque token is not a JWT, so the
default resolver would reject agent requests before the device authentication
code ever runs. `JwtOnlyBearerTokenResolver` only hands a token to the JWT
decoder when it has the three-part `header.payload.signature` shape; everything
else is left to the agent authentication path. This keeps a single
`SecurityFilterChain` for both credential types.

## Login throttling

Failed logins are rate limited in memory to blunt brute-force and password
spraying:

- five failed attempts for the same username within 15 minutes lock that
  username out,
- twenty failed attempts from the same client IP within 15 minutes lock that IP
  out (this catches spraying across many usernames),
- a locked-out request returns `429 Too Many Requests` with a `Retry-After`
  header in seconds and the body
  `Too many failed login attempts. Try again later.`,
- a successful login clears the failed-attempt counter for that username only;
  the IP counter is never cleared by a success, so one valid login does not
  reset an attacker's budget.

The counters live in a bounded, least-recently-used map (10,000 entries by
default) so a flood of distinct usernames or IPs cannot grow memory without
limit. The limits and window are configurable through
`sentinel.security.throttle.*` (see `application.properties`). Throttling is per
API instance and resets on restart; the single-server deployment in
[deployment.md](deployment.md) runs one instance.

Because the check runs before the password is verified, a correct password is
also rejected once the limit is hit.

## Agent authentication

Agents do not use JWTs. Each device gets its own opaque random token.

### Enrolment (first run)

1. The agent loads or creates its device id (`~/.sentinel-agent/agent-id`).
- If no token file exists yet, the agent reads `SENTINEL_ENROLLMENT_KEY` and
  sends it in the `X-Enrollment-Key` header to
  `POST /api/v1/devices/register`.
- If the key is missing the agent prints a clear message and exits with a
  non-zero status instead of starting.
- The API compares the presented key to the configured key in constant time and
  rejects a mismatch with `401`.
- On success the API creates the device, generates a random token, stores only
  its SHA-256 hash, and returns the token once, in the `deviceToken` field. This
  is the only time the token is ever returned.
- The agent writes the token (owner read/write only where the platform supports
  POSIX permissions) before doing anything else.

### Re-registration

Once a token file exists, the agent sends `Authorization: Bearer <token>` to
`POST /api/v1/devices/register`. The API authenticates the token, checks that it
belongs to the requested device, updates the device metadata, and returns
`deviceToken: null` (a token is never rotated implicitly).

### Heartbeats and metrics

`POST /api/v1/devices/heartbeat` and `POST /api/v1/metrics` require a valid
device token. The token must belong to the `uuid` in the request body; a valid
token for a different device is rejected with `403`.

### Revocation

An authenticated dashboard user can `POST
/api/v1/devices/{uuid}/revoke-token`. This clears the stored token hash, so the
agent's existing token stops working immediately (its next register, heartbeat,
or metrics call returns `401`). The device row and its metric history are kept.
To recover, an operator deletes `~/.sentinel-agent/device-token` and restarts
the agent, which re-enrols with the enrolment key.

### Agent behaviour on rejection

The agent never crashes on authentication errors. When the API rejects its token
with `401` or `403`, it prints a single message
("The API rejected this device's token. It may have been revoked. Delete
~/.sentinel-agent/device-token and enrol again") and keeps retrying without
spamming the log. The message is suppressed until a request succeeds again.
Missing-key and wrong-key situations at first enrolment produce a clear message
and a non-zero exit.

## Token and password storage

| Secret | Storage | Notes |
| --- | --- | --- |
| User password | `app_users.password_hash` (BCrypt) | Never logged, never returned. |
| Device token | `devices.token_hash` (SHA-256 hex) | Writable only through enrolment. Opaque random value, so a fast hash is appropriate (no dictionary attacks). |
| JWT signing key | Environment only | Never persisted by the application. |

`Device.tokenHash` is annotated so it is never serialized into any API response,
and integration tests assert that neither the raw token nor the hash appears in
device or metrics responses.

`devices.token_hash` has a unique index, so two devices can never share a token
hash. The column is nullable (a device whose token was revoked has no hash), and
both PostgreSQL and H2 allow any number of `NULL` values in a unique column, so a
device can be revoked and later re-enrol without a conflict. A repository test
asserts both properties.

## What is not covered yet

- **TLS in the application.** The API speaks plain HTTP; the deployment in
  [deployment.md](deployment.md) puts Caddy in front of it, which terminates
  HTTPS with an automatically obtained certificate and is the only service that
  publishes ports.
- **Token rotation.** Device tokens do not expire on their own; rotate them with
  revoke plus re-enrol.
- **Multiple users and roles.** Only the single `ADMIN` role exists, and there
  is no user management API.
- **Refresh tokens.** A JWT simply expires after its TTL and the user logs in
  again.
- **Distributed throttling.** Login throttling is per API instance; there is no
  shared store, so it does not coordinate across replicas.
- **Audit log.** Revocations and logins are not recorded.

## Reporting

This is an educational project. The bundled deployment adds TLS, private
networking, secret handling, and login throttling, but a hardened production
deployment should also add an audit trail and a secret manager.
