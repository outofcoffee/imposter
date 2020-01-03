#!/usr/bin/env bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CURRENT_VERSION="$( git describe --tags --exact-match || true )"

if [[ "${CURRENT_VERSION:0:1}" != "v" ]]; then
	echo "No release tag detected"
	exit
fi

echo "Detected release tag: ${CURRENT_VERSION}"
pushd ${SCRIPT_DIR}

./release-current.sh -y
