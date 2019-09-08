#!/usr/bin/env bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="${SCRIPT_DIR}/../"
DEFAULT_DISTRO_NAME="openapi"
JAVA_ARGS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"

function usage() {
  echo -e "Usage:\n  $( basename $0 ) <docker|java> [distro name]"
  exit 1
}

if [[ $# -lt 1 ]]; then
  usage
else
  LAUNCH_MODE="$1"
  DISTRO_NAME="${2:-${DEFAULT_DISTRO_NAME}}"
fi

pushd ${ROOT_DIR}

./gradlew shadowJar

case ${LAUNCH_MODE} in
  docker)
    export IMAGE_DIR="${DISTRO_NAME}"
    ./scripts/docker-build.sh

    docker run -ti --rm -p 8080:8080 \
      -v "$(pwd)/plugin/${DISTRO_NAME}/src/test/resources/config":/opt/imposter/config \
      -e JAVA_ARGS="${JAVA_ARGS}" \
      outofcoffee/imposter-${DISTRO_NAME}:dev
    ;;

  java)
    java ${JAVA_ARGS} \
      -jar distro/${DISTRO_NAME}/build/libs/imposter.jar \
      --configDir ./plugin/${DISTRO_NAME}/src/test/resources/config
    ;;
esac
