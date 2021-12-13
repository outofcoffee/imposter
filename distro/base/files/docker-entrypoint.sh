#!/usr/bin/env sh
set -e

if [ "${IMPOSTER_BOOT}" = "cli" ]; then
  # cli boot
  export IMPOSTER_CLI_LOG_LEVEL="${IMPOSTER_CLI_LOG_LEVEL:-INFO}"
  export IMPOSTER_LOG_LEVEL="${IMPOSTER_LOG_LEVEL:-DEBUG}"
  exec imposter up /opt/imposter/config

else
  # direct JVM boot
  if [ -z "${JAVA_OPTS}" ]; then
    export JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
  fi
  export JAVA_OPTS="$JAVA_ARGS $JAVA_OPTS"
  exec "/opt/imposter/bin/imposter" "$@"
fi
