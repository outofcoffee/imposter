#!/usr/bin/env bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="${SCRIPT_DIR}/../"
DEFAULT_PLUGIN_NAME="openapi"
JAVA_ARGS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"
IMPOSTER_LOG_LEVEL="DEBUG"

while getopts ":m:p:c:l:" opt; do
  case ${opt} in
    m )
      LAUNCH_MODE=$OPTARG
      ;;
    p )
      PLUGIN_NAME=$OPTARG
      ;;
    c )
      CONFIG_DIR=$OPTARG
      ;;
    l )
      IMPOSTER_LOG_LEVEL=$OPTARG
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
  echo -e "Usage:\n  $( basename $0 ) -m <docker|java> [-p plugin-name] [-c config-dir] [-l log-level]"
  exit 1
}

if [[ -z ${LAUNCH_MODE} ]]; then
  usage
else
  PLUGIN_NAME="${PLUGIN_NAME:-${DEFAULT_PLUGIN_NAME}}"
  CONFIG_DIR="${CONFIG_DIR:-$(pwd)/plugin/${PLUGIN_NAME}/src/test/resources/config}"
  CONFIG_DIR="$( cd ${CONFIG_DIR} && pwd )"
fi

pushd ${ROOT_DIR}

./gradlew shadowJar

case ${LAUNCH_MODE} in
  docker)
    export IMAGE_DIR="${PLUGIN_NAME}"
    ./scripts/docker-build.sh

    # consumed below
    export IMPOSTER_LOG_LEVEL

    docker run -ti --rm -p 8080:8080 \
      -v "${CONFIG_DIR}":/opt/imposter/config \
      -e IMPOSTER_LOG_LEVEL \
      -e JAVA_ARGS="${JAVA_ARGS}" \
      outofcoffee/imposter-${PLUGIN_NAME}:dev
    ;;

  java)
    java ${JAVA_ARGS} \
      -jar distro/${PLUGIN_NAME}/build/libs/imposter-${PLUGIN_NAME}.jar \
      --configDir ${CONFIG_DIR}
    ;;
esac
