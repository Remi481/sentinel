# Technology Stack

## Overview

Sentinel is built using modern, open-source technologies chosen for reliability, scalability, and cross-platform compatibility.

---

## Backend

### Java 21

Java provides excellent cross-platform support and is ideal for building both the monitoring agent and backend services.

### Spring Boot

Spring Boot is used to develop the REST API, authentication system, and business logic.

---

## Frontend

### React

React provides a responsive and component-based user interface for monitoring devices in real time.

---

## Database

### PostgreSQL

PostgreSQL stores users, devices, metrics, alerts, and audit logs.

---

## Monitoring Agent

### Java

The monitoring agent is written in Java so it can run on both Windows and Linux with minimal changes.

---

## Deployment

### Single Linux server

Sentinel deploys to one Linux server with no cloud dependency. Docker Compose
runs the API, PostgreSQL, and a reverse proxy on the same host, and a DNS name
plus open ports 80 and 443 are all that is required.

### Caddy

Caddy terminates TLS with automatically obtained and renewed certificates, serves
the built dashboard, and reverse-proxies `/api/*` to the API. It is the only
service that publishes ports.

---

## Containers

### Docker

Docker packages the API, agent, and dashboard as images, and Docker Compose wires
them together with PostgreSQL for consistent, repeatable deployments.

---

## Authentication

### JWT (JSON Web Tokens)

JWT will secure communication between users, agents, and the backend API.

---

## Version Control

### Git + GitHub

Git tracks project history, while GitHub hosts the repository, documentation, issues, and project board.

---

## Future Technologies

- Kubernetes
- Prometheus
- Grafana
- Redis

