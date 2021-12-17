#!/usr/bin/env ash
set -e

if [[ -z "${JAVA_OPTS}" ]]; then
  export JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
fi
export JAVA_OPTS

exec "/opt/imposter/bin/imposter" "$@"
