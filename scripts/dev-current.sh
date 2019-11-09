#!/usr/bin/env bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="${SCRIPT_DIR}/../"
DEFAULT_DISTRO_NAME="openapi"
JAVA_ARGS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"

while getopts ":m:p:c:" opt; do
  case ${opt} in
    m )
      LAUNCH_MODE=$OPTARG
      ;;
    p )
      DISTRO_NAME=$OPTARG
      ;;
    c )
      CONFIG_DIR=$OPTARG
      ;;
    \? )
      echo "Invalid option: $OPTARG" 1>&2
      ;;
    : )
      echo "Invalid option: $OPTARG requires an argument" 1>&2
      ;;
  esac
done
shift $((OPTIND -1))

function usage() {
  echo -e "Usage:\n  $( basename $0 ) -m <docker|java> [-p plugin-name] [-c config-dir] [-d]"
  exit 1
}

if [[ -z ${LAUNCH_MODE} ]]; then
  usage
else
  DISTRO_NAME="${DISTRO_NAME:-${DEFAULT_DISTRO_NAME}}"
  CONFIG_DIR="${CONFIG_DIR:-$(pwd)/plugin/${DISTRO_NAME}/src/test/resources/config}"
fi

pushd ${ROOT_DIR}

./gradlew shadowJar

case ${LAUNCH_MODE} in
  docker)
    export IMAGE_DIR="${DISTRO_NAME}"
    ./scripts/docker-build.sh

    docker run -ti --rm -p 8080:8080 \
      -v "${CONFIG_DIR}":/opt/imposter/config \
      -e JAVA_ARGS="${JAVA_ARGS}" \
      outofcoffee/imposter-${DISTRO_NAME}:dev
    ;;

  java)
    java ${JAVA_ARGS} \
      -jar distro/${DISTRO_NAME}/build/libs/imposter-${DISTRO_NAME}.jar \
      --configDir ${CONFIG_DIR}
    ;;
esac
