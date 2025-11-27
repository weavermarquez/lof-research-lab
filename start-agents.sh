#!/usr/bin/env bash
set -Eeuo pipefail

cd "$(dirname "$0")"

PORT="${AOR_PORT:-1974}"
RAMA_ROOT="${RAMA_ROOT:-/home/workspace/rama}"

AOR_READY_TIMEOUT="${AOR_READY_TIMEOUT:-180}"   # seconds to wait for UI
RECHECK="${RECHECK:-2}"                         # seconds between checks

wait_conductor_ready() {
  while true; do
    if out="$("$RAMA_ROOT"/rama conductorReady 2>/dev/null)"; then
      [ "$(printf '%s' "$out" | tr -d '\r')" = "true" ] && return 0
    fi
    sleep "$RECHECK"
  done
}

echo "[aor-wrapper] waiting for Rama Conductor READY..."
wait_conductor_ready
echo "[aor-wrapper] Conductor is READY, starting Agent-o-Rama..."

# In foreground: supervisord will log stdout/stderr and restart on failure
exec ./aor --rama "$RAMA_ROOT" --port "$PORT"
