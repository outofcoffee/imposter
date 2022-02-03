#!/usr/bin/env bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="$( cd ${SCRIPT_DIR}/../ && pwd )"

function start_engine() {
  cleanup_engine

  docker run --rm -id \
    -p 8080:8080 \
    -v "$ROOT_DIR/examples/openapi/simple:/opt/imposter/config" \
    --name imposter-test \
    "outofcoffee/imposter:$IMAGE_TAG"

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
  IMAGE_TAG="$1"
else
  echo -e "Usage:\n  $( basename $0 ) IMAGE_TAG"
  exit 1
fi

start_engine

(curl --fail http://localhost:8080/pets && test_passed) || test_failed

cleanup_engine
