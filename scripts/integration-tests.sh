#!/usr/bin/env bash
#
# Copyright (c) 2024.
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
ROOT_DIR="$( cd ${SCRIPT_DIR}/../ && pwd )"

function start_engine() {
  cleanup_engine

  docker run --rm -id \
    -p 8080:8080 \
    -v "${ROOT_DIR}/examples/openapi/simple:/opt/imposter/config" \
    --name imposter-test \
    "${DOCKER_IMAGE}"

  echo "Waiting for Imposter to come up..."
  while ! curl --fail --silent http://localhost:8080/system/status; do
    sleep 1
  done

  echo -e "\nImposter is up"
}

function cleanup_engine() {
  echo "Removing any existing imposter-test containers"
  docker rm -f imposter-test || true
}

function test_passed() {
  echo -e "\nIntegration tests passed"
}

function test_failed() {
  echo -e "\nIntegration tests failed"
}

if [[ $# -eq 1 ]]; then
  DOCKER_IMAGE="$1"
else
  echo -e "Usage:\n  $( basename $0 ) DOCKER_IMAGE"
  exit 1
fi

start_engine

(curl --fail http://localhost:8080/pets && test_passed) || test_failed

cleanup_engine
