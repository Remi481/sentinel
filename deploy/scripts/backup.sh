#!/usr/bin/env bash
#
# Dump the Sentinel PostgreSQL database to a timestamped gzip file.
#
#   deploy/scripts/backup.sh
#
# Environment overrides:
#   SENTINEL_BACKUP_DIR             output directory (default: deploy/backups)
#   SENTINEL_BACKUP_RETENTION_DAYS  delete dumps older than this (default: 7)
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
deploy_dir="$(cd "$script_dir/.." && pwd)"
backup_dir="${SENTINEL_BACKUP_DIR:-$deploy_dir/backups}"
retention_days="${SENTINEL_BACKUP_RETENTION_DAYS:-7}"

mkdir -p "$backup_dir"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
file="$backup_dir/sentinel-$timestamp.sql.gz"

cd "$deploy_dir"
docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB"' | gzip > "$file"

echo "Wrote $file"

find "$backup_dir" -name 'sentinel-*.sql.gz' -type f -mtime "+$retention_days" -delete
