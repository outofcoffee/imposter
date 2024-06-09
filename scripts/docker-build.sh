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

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../" && pwd)"
DOCKER_LOGIN_ARGS=""
IMAGE_REPOSITORY="outofcoffee/"
DEFAULT_IMAGE_DIRS=(
  "base"
  "core"
  "all"
  "distroless"
)
PUSH_IMAGES="false"

function usage() {
  echo -e "Usage:\n  $(basename $0) [-p <true|false> -e]"
  exit 1
}

if [[ -z "${IMAGE_DIR}" ]]; then
  IMAGE_DIRS=(
    "${DEFAULT_IMAGE_DIRS[@]}"
  )
else
  IMAGE_DIRS=(
    "base"
    "${IMAGE_DIR}"
  )
fi

while getopts "ep:" OPT; do
  case ${OPT} in
  e)
    DOCKER_LOGIN_ARGS="--email dummy@example.com"
    ;;
  p)
    PUSH_IMAGES="$OPTARG"
    ;;
  *)
    usage
    ;;
  esac
done
shift $((OPTIND - 1))

IMAGE_TAG="${1-dev}"

function get_image_names() { case $1 in
  core) echo "imposter imposter-openapi imposter-rest" ;;
  **) echo "imposter-$1" ;;
  esac }

function build_image() {
  local IMAGE_DIR="$1"
  local IMAGE_NAME="$2"
  local IMAGE_PATH="$3"

  if [[ "${IMAGE_DIR}" != "base" ]]; then
    BUILD_ARGS="--build-arg BASE_IMAGE_TAG=${IMAGE_TAG} --build-arg DISTRO_NAME=${IMAGE_DIR}"
  else
    BUILD_ARGS="--build-arg IMPOSTER_VERSION=$( ${SCRIPT_DIR}/get-version.sh )"
  fi

  if [[ "${CONTAINER_BUILDER}" == "buildx" ]]; then
    build_image_buildx "${IMAGE_NAME}" "${IMAGE_PATH}" "${BUILD_ARGS}"
  else
    build_image_embedded "${IMAGE_NAME}" "${IMAGE_PATH}" "${BUILD_ARGS}"
  fi
}

function build_image_buildx() {
  local IMAGE_NAME="$1"
  local IMAGE_PATH="$2"
  local BUILD_ARGS="$3"
  local FULL_IMAGE_NAME="${IMAGE_REPOSITORY}${IMAGE_NAME}:${IMAGE_TAG}"

  echo -e "\nUsing buildx for Docker image: ${FULL_IMAGE_NAME} [push: ${PUSH_IMAGES}]"

  if [[ "${PUSH_IMAGES}" == "true" ]]; then
    echo -e "\nBuilding multiplatform image: ${FULL_IMAGE_NAME}"
    docker buildx create --driver docker-container --use
    BUILD_ARGS="${BUILD_ARGS} --push --platform linux/amd64,linux/arm64"
  else
    docker buildx use default
    echo -e "\nBuilding single platform image: ${FULL_IMAGE_NAME}"
    BUILD_ARGS="${BUILD_ARGS} --load --platform linux/amd64"
  fi

  docker buildx build --file "${IMAGE_PATH}/Dockerfile" ${BUILD_ARGS} --tag "${FULL_IMAGE_NAME}" .
}

function build_image_embedded() {
  local IMAGE_NAME="$1"
  local IMAGE_PATH="$2"
  local BUILD_ARGS="$3"
  local FULL_IMAGE_NAME="${IMAGE_REPOSITORY}${IMAGE_NAME}:${IMAGE_TAG}"

  echo -e "\nUsing embedded Docker builder for image: ${FULL_IMAGE_NAME} [push: ${PUSH_IMAGES}]"
  docker build --file "${IMAGE_PATH}/Dockerfile" ${BUILD_ARGS} --tag "${FULL_IMAGE_NAME}" .

  if [[ "${PUSH_IMAGES}" == "true" ]]; then
    echo -e "\nPushing Docker image: ${FULL_IMAGE_NAME}"
    docker push "${FULL_IMAGE_NAME}"
  fi
}

function build_images() {
  local IMAGE_DIR="$1"
  echo -e "\nBuilding '${IMAGE_DIR}' image"

  for IMAGE_NAME in $(get_image_names ${IMAGE_DIR}); do
    build_image "${IMAGE_DIR}" "${IMAGE_NAME}" "distro/${IMAGE_DIR}"
  done
}

function docker_login() {
  if [[ "dev" == "${IMAGE_TAG}" ]]; then
    echo -e "\nSkipped registry login"
  else
    echo -e "\nLogging in to Docker registry..."
    echo "${DOCKER_PASSWORD}" | docker login --username "${DOCKER_USERNAME}" --password-stdin ${DOCKER_LOGIN_ARGS}
  fi
}

if [[ "dev" == "${IMAGE_TAG}" ]]; then
  echo -e "\nWill skip pushing dev image"
  PUSH_IMAGES="false"
fi

if [[ -z "${DOCKER_LOGIN}" && "${PUSH_IMAGES}" == "true" ]]; then
  DOCKER_LOGIN="true"
fi

if [[ "${DOCKER_LOGIN}" == "true" ]]; then
  docker_login
fi

cd "${ROOT_DIR}"

echo "Images to build: ${IMAGE_DIRS[*]}"

for IMAGE_DIR in "${IMAGE_DIRS[@]}"; do
  build_images "${IMAGE_DIR}"
done
