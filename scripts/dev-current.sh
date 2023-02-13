#!/usr/bin/env bash
#
# Copyright (c) 2023.
#
# This file is part of Imposter.
#
# "Commons Clause" License Condition v1.0
#
# The Software is provided to you by the Licensor under the License, as
# defined below, subject to the following condition.
#
# Without limiting other conditions in the License, the grant of rights
# under the License will not include, and the License does not grant to
# you, the right to Sell the Software.
#
# For purposes of the foregoing, "Sell" means practicing any or all of
# the rights granted to you under the License to provide to third parties,
# for a fee or other consideration (including without limitation fees for
# hosting or consulting/support services related to the Software), a
# product or service whose value derives, entirely or substantially, from
# the functionality of the Software. Any license notice or attribution
# required by the License must also include this Commons Clause License
# Condition notice.
#
# Software: Imposter
#
# License: GNU Lesser General Public License version 3
#
# Licensor: Peter Cornish
#
# Imposter is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Imposter is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
#

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="$( cd "${SCRIPT_DIR}"/../ && pwd )"
DEFAULT_LAUNCH_MODE="java"
DEFAULT_DISTRO_NAME="core"
IMPOSTER_LOG_LEVEL="DEBUG"
RUN_TESTS="true"
DEBUG_MODE="true"
SUSPEND_DEBUGGER="n"
MEASURE_PERF="false"
RECURSIVE_CONFIG="false"
PORT="8080"

while getopts "m:d:c:f:l:p:rst:z:" opt; do
  case ${opt} in
    m )
      LAUNCH_MODE=$OPTARG
      ;;
    d )
      DISTRO_NAME=$OPTARG
      ;;
    c )
      CONFIG_DIR=$OPTARG
      ;;
    f )
      MEASURE_PERF=$OPTARG
      ;;
    l )
      IMPOSTER_LOG_LEVEL=$OPTARG
      ;;
    p )
      PORT=$OPTARG
      ;;
    r )
      RECURSIVE_CONFIG="true"
      ;;
    s )
      SUSPEND_DEBUGGER="y"
      ;;
    t )
      RUN_TESTS=$OPTARG
      ;;
    z )
      DEBUG_MODE=$OPTARG
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
  echo -e "Usage:\n  $( basename $0 ) -c config-dir [-m <docker|java>] [-d distro-name] [-l log-level] [-t run-tests] [-r recursive-config] [-s suspend-debugger] [-z debug-mode]"
  exit 1
}

JAVA_TOOL_OPTIONS=
if [[ "$DEBUG_MODE" == "true" ]]; then
  JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=${SUSPEND_DEBUGGER},address=8000"
fi

if [[ -z "${CONFIG_DIR}" ]]; then
  usage
else
  DISTRO_NAME="${DISTRO_NAME:-${DEFAULT_DISTRO_NAME}}"
  CONFIG_DIR="$( cd ${CONFIG_DIR} && pwd )"
fi

if [[ -z "$LAUNCH_MODE" ]]; then
  LAUNCH_MODE="${DEFAULT_LAUNCH_MODE}"
fi

pushd ${ROOT_DIR}

GRADLE_ARGS=
if [[ "$RUN_TESTS" == "false" ]]; then
  GRADLE_ARGS="-xtest"
fi
# using installDist instead of dist to avoid unneeded shadow JAR for local dev
./gradlew installDist ${GRADLE_ARGS}

if [[ "true" == "${MEASURE_PERF}" ]]; then
  ./gradlew :tools:perf-monitor:shadowJar
  JAVA_TOOL_OPTIONS="-javaagent:${ROOT_DIR}/tools/perf-monitor/build/libs/imposter-perf-monitor.jar=/tmp/imposter-method-perf.csv ${JAVA_TOOL_OPTIONS}"
fi

if [[ "true" == "${RECURSIVE_CONFIG}" ]]; then
  export IMPOSTER_CONFIG_SCAN_RECURSIVE="true"
fi

# consumed below
export IMPOSTER_LOG_LEVEL
export JAVA_TOOL_OPTIONS

case ${LAUNCH_MODE} in
  docker)
    export IMAGE_DIR="${DISTRO_NAME}"
    ./scripts/docker-build.sh

    case "${DISTRO_NAME}" in
    core)
      DOCKER_IMAGE_NAME="imposter"
      ;;
    **)
      DOCKER_IMAGE_NAME="imposter-${DISTRO_NAME}"
      ;;
    esac

    docker run -ti --rm -p $PORT:8080 \
      -v "${CONFIG_DIR}":/opt/imposter/config \
      -e IMPOSTER_LOG_LEVEL \
      -e JAVA_TOOL_OPTIONS \
      "outofcoffee/${DOCKER_IMAGE_NAME}:dev"
    ;;

  java)
    cd "distro/${DISTRO_NAME}/build/install/imposter"
    "./bin/imposter" "--listenPort=${PORT}" "--configDir=${CONFIG_DIR}"
    ;;
esac
