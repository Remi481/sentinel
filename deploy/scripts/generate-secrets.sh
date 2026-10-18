#!/usr/bin/env bash
#
# Generate strong random values for the Sentinel deploy secrets.
#
#   ./generate-secrets.sh              print one NAME=value line per secret, no files touched
#   ./generate-secrets.sh --write      create deploy/.env and fill the secrets in place
#   ./generate-secrets.sh --write --force
#                                      overwrite values that are already real
#
# --write never prints the values it generates; it prompts for the admin password
# on hidden input and writes deploy/.env with LF line endings. If SENTINEL_DOMAIN
# is empty or still the example placeholder, it is set to localhost so a local
# trial works without editing the file.
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
deploy_dir="$(cd "$script_dir/.." && pwd)"
env_file="$deploy_dir/.env"
example_file="$deploy_dir/.env.example"
placeholder="REPLACE_WITH_GENERATED_VALUE"
legacy_placeholder="change-me"
domain_placeholder="sentinel.example.com"
local_domain="localhost"
min_password_length=12
secret_keys=(SENTINEL_DB_PASSWORD SENTINEL_JWT_SECRET SENTINEL_ENROLLMENT_KEY)
target_keys=(SENTINEL_DB_PASSWORD SENTINEL_JWT_SECRET SENTINEL_ENROLLMENT_KEY SENTINEL_ADMIN_PASSWORD)

usage() {
    echo "Usage: $(basename "$0") [--write] [--force]"
    echo
    echo "  (no options)  print one NAME=value line per secret; no file is written"
    echo "  --write       create deploy/.env from .env.example if missing and"
    echo "                replace the secret lines in place without printing them"
    echo "  --force       with --write, overwrite values that are already real"
}

write_mode=0
force=0
for arg in "$@"; do
    case "$arg" in
        --write) write_mode=1 ;;
        --force) force=1 ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $arg" >&2
            usage >&2
            exit 2
            ;;
    esac
done

if ! command -v openssl >/dev/null 2>&1; then
    echo "openssl is required to generate secrets" >&2
    exit 1
fi

random_secret() {
    openssl rand -base64 32 | tr -d '\n'
}

if [ "$write_mode" -eq 0 ]; then
    echo
    echo 'Sentinel deploy secrets'
    echo '======================='
    echo 'Paste these lines into deploy/.env (copy .env.example to .env first).'
    echo 'Each value is a fresh 32-byte random secret.'
    echo
    echo "SENTINEL_DB_PASSWORD=$(random_secret)"
    echo "SENTINEL_JWT_SECRET=$(random_secret)"
    echo "SENTINEL_ENROLLMENT_KEY=$(random_secret)"
    echo "SENTINEL_ADMIN_PASSWORD=$(random_secret)"
    echo
    echo 'Also set in deploy/.env:'
    echo '  SENTINEL_DOMAIN          localhost for a local trial; set your DNS name on a real server'
    echo '  SENTINEL_ADMIN_USERNAME  the bootstrap admin username'
    echo
    echo 'Save every printed value in a password manager now. They are not stored anywhere by this script.'
    echo 'The enrolment key is needed again for every new agent you enrol, not just once.'
    exit 0
fi

if [ ! -f "$example_file" ]; then
    echo "Missing template: $example_file" >&2
    exit 1
fi

if [ ! -f "$env_file" ]; then
    cp "$example_file" "$env_file"
    echo "Created $env_file from .env.example"
fi

get_value() {
    local key="$1" line
    while IFS= read -r line || [ -n "$line" ]; do
        line="${line%$'\r'}"
        case "$line" in
            "$key="*)
                printf '%s' "${line#*=}"
                return 0
                ;;
        esac
    done < "$env_file"
    return 0
}

set_value() {
    local key="$1" value="$2" tmp found=0 line
    tmp="$(mktemp)"
    while IFS= read -r line || [ -n "$line" ]; do
        line="${line%$'\r'}"
        case "$line" in
            "$key="*)
                printf '%s=%s\n' "$key" "$value"
                found=1
                ;;
            *)
                printf '%s\n' "$line"
                ;;
        esac
    done < "$env_file" > "$tmp"
    if [ "$found" -eq 0 ]; then
        printf '%s=%s\n' "$key" "$value" >> "$tmp"
    fi
    mv "$tmp" "$env_file"
}

is_real_value() {
    local value="$1"
    [ -n "$value" ] || return 1
    [ "$value" != "$placeholder" ] || return 1
    [ "$value" != "$legacy_placeholder" ] || return 1
    return 0
}

# An empty or placeholder domain is set to localhost so a local trial works
# without editing the file. A real DNS name is left alone.
domain_set=0
domain_value="$(get_value SENTINEL_DOMAIN)"
if [ -z "$domain_value" ] || [ "$domain_value" = "$domain_placeholder" ]; then
    set_value SENTINEL_DOMAIN "$local_domain"
    domain_set=1
    echo 'SENTINEL_DOMAIN was empty or a placeholder, set it to localhost for a local trial.'
fi

already_set=()
for key in "${target_keys[@]}"; do
    if is_real_value "$(get_value "$key")"; then
        already_set+=("$key")
    fi
done

if [ "${#already_set[@]}" -gt 0 ] && [ "$force" -eq 0 ]; then
    echo
    echo "Refusing to overwrite real values already in $env_file :" >&2
    for key in "${already_set[@]}"; do
        echo "  $key" >&2
    done
    echo 'Re-run with --force to replace them, or edit the file by hand.' >&2
    exit 1
fi

admin_password=""
if [ -t 0 ]; then
    while true; do
        if ! IFS= read -r -s -p "New SENTINEL_ADMIN_PASSWORD (hidden, at least $min_password_length characters): " admin_password; then
            echo
            echo 'No admin password was read from the input.' >&2
            exit 1
        fi
        echo
        if [ "${#admin_password}" -ge "$min_password_length" ]; then
            break
        fi
        echo "SENTINEL_ADMIN_PASSWORD must be at least $min_password_length characters, try again."
    done
else
    if ! IFS= read -r admin_password; then
        echo 'No admin password was read from the redirected input.' >&2
        exit 1
    fi
    if [ "${#admin_password}" -ge "$min_password_length" ]; then
        :
    else
        echo "SENTINEL_ADMIN_PASSWORD must be at least $min_password_length characters." >&2
        exit 1
    fi
fi

for key in "${secret_keys[@]}"; do
    set_value "$key" "$(random_secret)"
done
set_value SENTINEL_ADMIN_PASSWORD "$admin_password"

admin_username="$(get_value SENTINEL_ADMIN_USERNAME)"
username_kept=0
if is_real_value "$admin_username"; then
    username_kept=1
else
    set_value SENTINEL_ADMIN_USERNAME "admin"
fi

chmod 600 "$env_file"

echo
echo "Wrote $env_file (values are not shown):"
echo '  SENTINEL_DB_PASSWORD, SENTINEL_JWT_SECRET, SENTINEL_ENROLLMENT_KEY  fresh random values'
echo '  SENTINEL_ADMIN_PASSWORD                                             the password you typed'
if [ "$username_kept" -eq 1 ]; then
    echo '  SENTINEL_ADMIN_USERNAME                                             kept as it is'
else
    echo '  SENTINEL_ADMIN_USERNAME                                             set to admin'
fi
if [ "$domain_set" -eq 0 ]; then
    echo '  SENTINEL_DOMAIN                                                     kept as it is'
fi
echo
echo 'Next: run'
echo "  cd \"$deploy_dir\" && docker compose up -d --build"
echo
echo 'Then open https://localhost (not 127.0.0.1 or an IP, which the certificate'
echo 'does not cover). On a real server, set SENTINEL_DOMAIN to its DNS name first.'
echo
echo 'WARNING: PostgreSQL only reads SENTINEL_DB_PASSWORD when the database volume is'
echo 'first created. Changing it later leaves the old password inside the volume and'
echo 'breaks the connection. For a trial, run: docker compose down -v'
