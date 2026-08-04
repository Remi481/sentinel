# Sentinel Domain Model

## Overview

The Sentinel platform revolves around a small number of core entities that represent devices, users, monitoring data, and security events.

---

# User

Represents a person who can access the Sentinel dashboard.

Properties:

- id
- firstName
- lastName
- email
- passwordHash
- role
- createdAt

---

# Device

Represents a monitored computer.

Properties:

- id
- uuid
- hostname
- friendlyName
- operatingSystem
- ipAddress
- status
- trustLevel
- firstSeen
- lastSeen

---

# Metric

Represents a snapshot of system performance.

Properties:

- id
- cpuUsage
- memoryUsage
- diskUsage
- networkUsage
- timestamp

---

# Alert

Represents an important event.

Properties:

- id
- severity
- title
- description
- createdAt
- resolved

---

# Audit Log

Records important system actions.

Properties:

- id
- user
- action
- timestamp

---

## Relationships

A User can manage many Devices.

A Device produces many Metrics.

A Device can generate many Alerts.

A User creates many Audit Log entries.
