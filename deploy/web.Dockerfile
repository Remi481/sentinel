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
COPY deploy/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist /srv
