#!/bin/sh
set -e

# Decide whether to start local Redis: only when REDIS_URL is empty or points to localhost/127.0.0.1
START_LOCAL_REDIS=false
if [ -z "$REDIS_URL" ]; then
  START_LOCAL_REDIS=true
elif echo "$REDIS_URL" | grep -Eq '^redis://(127\.0\.0\.1|localhost|$|:|/)' ; then
  START_LOCAL_REDIS=true
fi

if [ "$START_LOCAL_REDIS" = "true" ]; then
  echo "[entrypoint] Starting local Redis on 127.0.0.1:6379"
  redis-server --bind 127.0.0.1 --port 6379 --save "" --appendonly no &
else
  echo "[entrypoint] Using external Redis at $REDIS_URL"
fi

echo "[entrypoint] Starting API"
node dist/main.js &
API_PID=$!

if [ "${QUEUE_ENABLED:-true}" != "false" ]; then
  echo "[entrypoint] Starting worker"
  node dist/worker.js &
  WORKER_PID=$!
else
  echo "[entrypoint] Worker disabled (QUEUE_ENABLED=false)"
fi

# Wait for processes
wait $API_PID ${WORKER_PID:-}
