# ADR-001: Project Vision and Technology Direction

## Status

Accepted

---

## Date

2026-08-04

---

## Context

Sentinel is intended to become a cloud-native infrastructure monitoring and security platform.

The platform must support:

- Windows
- Linux
- Cloud deployment
- Secure communication
- Future scalability

The project will also serve as a professional portfolio demonstrating software engineering, cloud computing, and cybersecurity skills.

---

## Decision

Sentinel will be built using a modern Java ecosystem.

The system will consist of:

- Java monitoring agent
- Spring Boot backend
- PostgreSQL database
- React dashboard
- Docker containers
- AWS deployment

---

## Why this decision?

### Cross-platform support

Java allows the monitoring agent to run on both Windows and Linux.

---

### Enterprise ecosystem

Spring Boot is widely used in industry.

---

### Cloud ready

AWS integrates well with Docker and Java.

---

### Security

Java provides mature security libraries.

---

### Portfolio value

This technology stack demonstrates backend development, cloud computing, DevOps, and cybersecurity.

---

## Alternatives Considered

### Python

Pros

- Easy development

Cons

- Packaging for Windows/Linux can be more difficult for long-running desktop agents.

---

### Go

Pros

- Very fast

Cons

- Smaller ecosystem for this educational goal.

---

### .NET

Pros

- Excellent Windows support

Cons

- Less ideal for cross-platform educational objectives.

---

## Consequences

Benefits

- Strong cross-platform compatibility.
- Enterprise-ready architecture.
- Easy future expansion.

Trade-offs

- Slightly steeper learning curve.
- More initial setup than scripting languages.
