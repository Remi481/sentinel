#!/usr/bin/env bash
#
# Regression check for forwarding-header spoofing of the login throttle.
#
# Caddy is the only ingress and the API trusts the proxy
# (server.forward-headers-strategy=framework), so the API derives the client IP
# from the X-Forwarded-For / Forwarded headers. If a client can forge one of
# those headers it gets a fresh throttle key on every request, and the per-IP
# limit never fires.
#
# This script sends 25 wrong logins for 25 different usernames, each with a
# different forged Forwarded, X-Forwarded-For and X-Real-IP value, and requires
# the per-address limit to stop the flood. Run it after `docker compose up -d`.
#
#   deploy/scripts/check-forged-headers.sh
#   SENTINEL_BASE_URL=https://your-domain deploy/scripts/check-forged-headers.sh
#
# Environment overrides:
#   SENTINEL_BASE_URL  base URL (default: https://localhost)
#   SENTINEL_LIMIT     per-address limit to expect (default: 20)
#   SENTINEL_REQUESTS  number of requests to send (default: 25)
#   SENTINEL_INSECURE  set to 0 to verify TLS (default: 1, uses curl -k)
set -euo pipefail

base_url="${SENTINEL_BASE_URL:-https://localhost}"
limit="${SENTINEL_LIMIT:-20}"
requests="${SENTINEL_REQUESTS:-25}"
insecure="${SENTINEL_INSECURE:-1}"

curl_opts=(-s -o /dev/null -w '%{http_code}' --max-time 10)
if [ "$insecure" = "1" ]; then
  curl_opts+=(-k)
fi

# A unique suffix keeps the per-username limit out of the way, so this check
# targets the per-address limit only and can be re-run safely.
run_id="$(date -u +%s)-$$"

unauthorized=0
too_many=0
statuses=""
first_status=""
for n in $(seq 1 "$requests"); do
  code=$(curl "${curl_opts[@]}" \
    -H "Forwarded: for=10.9.9.$n" \
    -H "X-Forwarded-For: 10.9.9.$n" \
    -H "X-Real-IP: 10.9.9.$n" \
    -H "Content-Type: application/json" \
    --data "{\"username\":\"forged-$run_id-$n\",\"password\":\"wrong\"}" \
    "$base_url/api/v1/auth/login")
  statuses="$statuses $code"
  if [ "$n" -eq 1 ]; then
    first_status="$code"
  fi
  case "$code" in
    401) unauthorized=$((unauthorized + 1)) ;;
    429) too_many=$((too_many + 1)) ;;
  esac
done

echo "statuses:$statuses"
echo "401: $unauthorized  429: $too_many  (limit: $limit of $requests)"

if [ "$too_many" -lt 1 ]; then
  echo "FAIL: the per-address limit never fired, so a forged forwarding header changed the throttle key" >&2
  exit 1
fi
if [ "$first_status" = "429" ]; then
  echo "FAIL: every request was throttled on the first run; is the address already locked out?" >&2
  exit 1
fi

echo "OK: forged forwarding headers did not change the throttle key"
