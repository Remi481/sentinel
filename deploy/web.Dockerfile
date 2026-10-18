# Build the dashboard and serve it (with the API reverse proxy and automatic
# HTTPS) from Caddy. Build context is the repository root:
#   docker build -f deploy/web.Dockerfile .
FROM node:22-alpine AS build
WORKDIR /app

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

FROM caddy:2-alpine AS runtime

# The base image ships no unprivileged user and runs as root. Create one so
# Caddy can run without root; give it ownership of the writable directories.
# The upstream caddy binary carries a cap_net_bind_service file capability that
# makes exec fail under cap_drop: ALL, so strip it; low ports are bound through
# the net.ipv4.ip_unprivileged_port_start sysctl instead (see
# docker-compose.yml).
RUN apk add --no-cache libcap \
    && setcap -r /usr/bin/caddy \
    && addgroup -S -g 10001 caddy \
    && adduser -S -D -H -u 10001 -G caddy caddy \
    && mkdir -p /data /config /srv \
    && chown -R caddy:caddy /data /config /srv /etc/caddy

COPY deploy/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist /srv
RUN chown -R caddy:caddy /srv /etc/caddy

USER caddy
