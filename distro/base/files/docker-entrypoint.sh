#!/usr/bin/env sh
set -e

if [ -z "${JAVA_OPTS}" ]; then
  export JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
fi

exec java ${JAVA_ARGS} ${JAVA_OPTS} -jar /opt/imposter/lib/imposter-${DISTRO_NAME}.jar "$@"
