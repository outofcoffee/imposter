#!/usr/bin/env bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="${SCRIPT_DIR}/../"

IMAGES=(
	outofcoffee/imposter
	outofcoffee/imposter-base
	outofcoffee/imposter-openapi
	outofcoffee/imposter-rest
)

cd "${ROOT_DIR}"

if [[ -z "${CURRENT_VERSION}" ]]; then
  CURRENT_VERSION="$( git describe --tags --exact-match )"

  if [[ "${CURRENT_VERSION:0:1}" == "v" ]]; then
    CURRENT_VERSION="$( echo "${CURRENT_VERSION}" | cut -c 2- )"
  fi
fi

if [[ -z "${CONFIRMATION}" ]]; then
    read -p "Ready to tag latest as ${CURRENT_VERSION} (y/N)?" CONFIRMATION
fi
if [[ "y" != "${CONFIRMATION}" ]]; then
    exit 1
fi

for IMAGE_NAME in "${IMAGES[@]}"; do
  echo -e "\nPulling ${IMAGE_NAME}:latest..."
	docker pull "${IMAGE_NAME}:latest"
	docker tag "${IMAGE_NAME}:latest" "${IMAGE_NAME}:$CURRENT_VERSION"

  echo -e "\nPushing ${IMAGE_NAME}:$CURRENT_VERSION..."
	docker push "${IMAGE_NAME}:$CURRENT_VERSION"
done
