#!/usr/bin/env bash
#
# Create deploy/.env from .env.example and fill in strong random secrets.
# Existing non-placeholder values are left untouched, so it is safe to run again.
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
deploy_dir="$(cd "$script_dir/.." && pwd)"
env_file="$deploy_dir/.env"
example_file="$deploy_dir/.env.example"
placeholder="change-me"

if [ ! -f "$env_file" ]; then
    cp "$example_file" "$env_file"
    echo "Created $env_file from .env.example"
fi

if ! command -v openssl >/dev/null 2>&1; then
    echo "openssl is required to generate secrets" >&2
    exit 1
fi

random_secret() {
    openssl rand -base64 32 | tr -d '\n'
}

set_secret() {
    local key="$1"
    local current
    current="$(sed -n "s/^${key}=//p" "$env_file" | head -n 1)"

    if [ -n "$current" ] && [ "$current" != "$placeholder" ]; then
        echo "Kept existing $key"
        return
    fi

    local value tmp
    value="$(random_secret)"
    tmp="$(mktemp)"
    awk -v key="$key" -v value="$value" '
        BEGIN { replaced = 0 }
        index($0, key "=") == 1 { print key "=" value; replaced = 1; next }
        { print }
        END { if (!replaced) print key "=" value }
    ' "$env_file" > "$tmp"
    mv "$tmp" "$env_file"
    echo "Generated $key"
}

set_secret SENTINEL_DB_PASSWORD
set_secret SENTINEL_JWT_SECRET
set_secret SENTINEL_ENROLLMENT_KEY
set_secret SENTINEL_ADMIN_PASSWORD

chmod 600 "$env_file"

echo
echo "Secrets written to $env_file"
echo "Set SENTINEL_DOMAIN and SENTINEL_ADMIN_USERNAME there, then run:"
echo "  cd \"$deploy_dir\" && docker compose up -d --build"
