The Agent:

This is the software installed on a computer. Its job is only to collect information.
The agent never stores data permanently and insteads sends everything to the backend.

Spring Boot Backend:

Think of it as the brain, receives data, validates it, stores, creates alerts and responds to the dashboard. Everything flows through it.

PostgreSQL:

The permanent storage. Nothing else writes directly to the database, only the backend.

Dashboard:

Never talks directly to agents. Dashboard to Backend to Database. More secure

Authentication:

Every request requires authentication.
Agents will also authenticate using their own credentials.

Data Flow:

Windows/Linux Starts

to

Sentinel Agent Starts

to

Collect Device Information

to

Authenticate

to

Send to Backend

to

Backend Validates

to

Store in Database

to

Dashboard Updates

Device Registration:

Agent

to

Register Device

to

Backend

to

Generate Device ID

to

Save Device

to

Return Authentication Token

And then the next time the device connects:

Device ID

+

Authentication Token

to

Backend

to

Accepted

No need to register again.

Offline Detection:

In the instance your laptop loses power. The backend expects a heartbeat every 30 seconds. If no heartbeat arrives after 90 seconds:

An alert generated and the dashboard is updated.


