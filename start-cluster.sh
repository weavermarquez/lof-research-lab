#!/usr/bin/env bash
set -Eeuo pipefail

cd "$(dirname "$0")"

echo "[rama-wrapper] starting Rama single-node cluster..."

# 1) Dev Zookeeper (background) + quick sanity check
./rama devZookeeper &
ZK_PID=$!
echo "[rama-wrapper] devZookeeper started with pid=$ZK_PID"
sleep 1
if ! kill -0 "$ZK_PID" 2>/dev/null; then
  echo "[rama-wrapper] devZookeeper died immediately; aborting"
  exit 1
fi

# 2) Rama Supervisor (background) + quick sanity check
./rama supervisor &
SUP_PID=$!
echo "[rama-wrapper] supervisor started with pid=$SUP_PID"
sleep 1
if ! kill -0 "$SUP_PID" 2>/dev/null; then
  echo "[rama-wrapper] supervisor died immediately; aborting"
  exit 1
fi

# 3) Optional: log numSupervisors (best-effort only)
if out=$(./rama numSupervisors 2>/dev/null | tr -d '\r'); then
  case "$out" in
    ''|*[!0-9]*) ;;
    *)
      echo "[rama-wrapper] numSupervisors currently reported: $out"
      ;;
  esac
fi

# 4) Conductor in foreground
echo "[rama-wrapper] starting conductor in foreground..."
exec ./rama conductor
