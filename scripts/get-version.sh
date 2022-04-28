#!/usr/bin/env bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="${SCRIPT_DIR}/../"

cat "${ROOT_DIR}/gradle.properties" | grep projectVersion | sed 's/projectVersion=//g'
