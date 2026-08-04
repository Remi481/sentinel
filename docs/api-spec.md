# Sentinel API Specification (Version 1)

## Authentication

POST /api/auth/login

Description

Authenticate a user and return a JWT.

---

## Devices

POST /api/devices/register

Description

Register a new monitoring agent.

---

GET /api/devices

Description

Retrieve all registered devices.

---

GET /api/devices/{id}

Description

Retrieve a specific device.

---

## Metrics

POST /api/metrics

Description

Submit performance metrics from an agent.

---

GET /api/metrics/{deviceId}

Description

Retrieve metrics for a device.

---

## Alerts

GET /api/alerts

Description

Retrieve active alerts.

---

POST /api/alerts

Description

Create an alert.

---

PATCH /api/alerts/{id}

Description

Mark an alert as resolved.

---

## Health

GET /api/health

Description

Verify that the backend is running.
