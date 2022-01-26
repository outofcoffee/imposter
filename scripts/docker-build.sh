#!/usr/bin/env bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="$( cd "${SCRIPT_DIR}/../" && pwd )"
DOCKER_LOGIN_ARGS=""
IMAGE_REPOSITORY="outofcoffee/"
DEFAULT_IMAGE_DIRS=(
    "base"
    "core"
    "openapi"
    "rest"
    "all"
)
PUSH_IMAGES="true"

function usage() {
  echo -e "Usage:\n  $( basename $0 ) [-p <true|false> -e]"
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
        e) DOCKER_LOGIN_ARGS="--email dummy@example.com"
        ;;
        p) PUSH_IMAGES="$OPTARG"
        ;;
        *) usage
        ;;
    esac
done
shift $((OPTIND-1))

IMAGE_TAG="${1-dev}"

function get_image_names() { case $1 in
    core) echo "imposter" ;;
    **) echo "imposter-$1" ;;
esac }

function build_image()
{
    IMAGE_DIR="$1"
    IMAGE_NAME="$2"
    IMAGE_PATH="$3"
    FULL_IMAGE_NAME="${IMAGE_REPOSITORY}${IMAGE_NAME}:${IMAGE_TAG}"

    if [[ "${IMAGE_DIR}" != "base" ]]; then
        BUILD_ARGS="--build-arg BASE_IMAGE_TAG=${IMAGE_TAG} --build-arg DISTRO_NAME=${IMAGE_DIR}"
    else
        BUILD_ARGS=""
    fi

    echo -e "\nBuilding Docker image: ${IMAGE_NAME}"
    docker build --file ${IMAGE_PATH}/Dockerfile ${BUILD_ARGS} --tag ${FULL_IMAGE_NAME} .
}

function push_image()
{
    IMAGE_NAME="$1"
    FULL_IMAGE_NAME="${IMAGE_REPOSITORY}${IMAGE_NAME}:${IMAGE_TAG}"

    echo -e "\nPushing Docker image: ${IMAGE_NAME}"
    docker push ${FULL_IMAGE_NAME}
}

function build_images()
{
    IMAGE_DIR="$1"
    echo -e "\nBuilding '${IMAGE_DIR}' image"

    for IMAGE_NAME in $( get_image_names ${IMAGE_DIR} ); do
        build_image ${IMAGE_DIR} ${IMAGE_NAME} "distro/${IMAGE_DIR}"
    done
}

function push_images()
{
    IMAGE_DIR="$1"

    for IMAGE_NAME in $( get_image_names ${IMAGE_DIR} ); do
        if [[ "dev" == "${IMAGE_TAG}" ]]; then
            echo -e "\nSkipped pushing dev image"
        else
            push_image ${IMAGE_NAME}
        fi
    done
}

function login() {
    if [[ "dev" == "${IMAGE_TAG}" ]]; then
        echo -e "\nSkipped registry login"
    else
        echo -e "\nLogging in to Docker registry..."
        echo ${DOCKER_PASSWORD} | docker login --username "${DOCKER_USERNAME}" --password-stdin ${DOCKER_LOGIN_ARGS}
    fi
}

if [[ "${PUSH_IMAGES}" == "true" ]]; then
  login
fi

cd ${ROOT_DIR}

echo "Images to build: ${IMAGE_DIRS[*]}"
for IMAGE_DIR in "${IMAGE_DIRS[@]}"; do
    build_images ${IMAGE_DIR}
    if [[ "${PUSH_IMAGES}" == "true" ]]; then
        push_images ${IMAGE_DIR}
    fi
done
