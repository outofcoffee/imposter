#!/usr/bin/env ash
set -e

function boot_direct_jvm() {
    exec "/opt/imposter/bin/imposter" "$@"
}

function boot_cli() {
  export IMPOSTER_CLI_LOG_LEVEL="${IMPOSTER_CLI_LOG_LEVEL:-INFO}"
  export IMPOSTER_LOG_LEVEL="${IMPOSTER_LOG_LEVEL:-DEBUG}"

  CONFIG_DIR="/opt/imposter/config"
  IMPOSTER_ARGS=
  while [ $# -gt 0 ]; do
    case $1 in
    "--configDir")
      shift 1
      CONFIG_DIR="$1"
      ;;
    "--listenPort")
      shift 1
      IMPOSTER_ARGS="${IMPOSTER_ARGS} --port $1"
      ;;
    esac
    shift 1
  done
  exec imposter up ${IMPOSTER_ARGS} "${CONFIG_DIR}"
}

if [[ -z "${JAVA_OPTS}" ]]; then
  export JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
fi
export JAVA_OPTS="$JAVA_ARGS $JAVA_OPTS"

if [[ "${IMPOSTER_BOOT}" == "classic" ]]; then
  boot_direct_jvm "$@"
else
  boot_cli "$@"
fi
