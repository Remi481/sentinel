# Sentinel

> A cloud-native infrastructure monitoring and security platform for Windows and Linux.

---

## Overview

Sentinel is a cross-platform infrastructure monitoring platform designed to provide real-time observability, device health monitoring, and security insights across Windows and Linux systems.

The platform consists of lightweight monitoring agents, a secure backend API, and a modern web dashboard that together enable centralized monitoring of distributed environments.

---

## Goals

- Monitor Windows and Linux devices in real time
- Collect system performance metrics
- Detect security-related events
- Provide centralized infrastructure visibility
- Support hybrid environments (on-premises and cloud)
- Deploy using AWS cloud services

---

## Planned Features

### Device Management

- Device registration
- Device inventory
- Health status
- Device tagging

### Monitoring

- CPU usage
- Memory usage
- Disk usage
- Network statistics
- Running processes

### Security

- Authentication
- JWT authorization
- Audit logging
- Security alerts

### Dashboard

- Live metrics
- Device overview
- Alert management
- Historical trends

---

## Architecture

```text
Windows Agent
        │
Linux Agent
        │
     HTTPS API
        │
Spring Boot Backend
        │
   PostgreSQL
        │
 React Dashboard
```

---

## Technology Stack

| Layer | Technology |
|--------|------------|
| Backend | Java 21 + Spring Boot |
| Frontend | React |
| Database | PostgreSQL |
| Cloud | AWS |
| Containerization | Docker |
| Authentication | JWT |
| Build Tool | Maven |

---

## Roadmap

- [x] Repository created
- [x] Project structure
- [ ] Backend API
- [ ] Monitoring Agent
- [ ] Dashboard
- [ ] AWS Deployment
- [ ] Version 1.0

---

## License

This project is licensed under the MIT License.
