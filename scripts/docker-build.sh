#!/usr/bin/env bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="$( cd "${SCRIPT_DIR}/../" && pwd )"
DOCKER_LOGIN_ARGS=""
IMAGE_REPOSITORY="outofcoffee/"
IMAGE_DIRS=(
    "base"
    "hbase"
    "openapi"
    "rest"
    "sfdc"
)

while getopts "e" OPT; do
    case ${OPT} in
        e) DOCKER_LOGIN_ARGS="--email dummy@example.com"
        ;;
    esac
done
shift $((OPTIND-1))

IMAGE_TAG="${1-dev}"

function getImageNames() { case $1 in
    base) echo "imposter" ;;
    **) echo "imposter-$1" ;;
esac }

function buildImage()
{
    IMAGE_NAME="$1"
    IMAGE_PATH="$2"
    FULL_IMAGE_NAME="${IMAGE_REPOSITORY}${IMAGE_NAME}:${IMAGE_TAG}"

    if [[ "${IMAGE_NAME}" != "imposter" ]]; then
        BUILD_ARGS="--build-arg BASE_IMAGE_TAG=${IMAGE_TAG}"
    else
        BUILD_ARGS=""
    fi

    echo -e "\nBuilding Docker image: ${IMAGE_NAME}"
    docker build --file ${IMAGE_PATH}/Dockerfile ${BUILD_ARGS} --tag ${FULL_IMAGE_NAME} .
}

function pushImage()
{
    IMAGE_NAME="$1"
    FULL_IMAGE_NAME="${IMAGE_REPOSITORY}${IMAGE_NAME}:${IMAGE_TAG}"

    echo -e "\nPushing Docker image: ${IMAGE_NAME}"
    docker push ${FULL_IMAGE_NAME}
}

function buildPushImage()
{
    IMAGE_DIR="$1"
    echo -e "\nBuilding '${IMAGE_DIR}' image"

    for IMAGE_NAME in $( getImageNames ${IMAGE_DIR} ); do
        buildImage ${IMAGE_NAME} "docker/${IMAGE_DIR}"

        if [[ "dev" == "${IMAGE_TAG}" ]]; then
            echo -e "\nSkipped pushing dev image"
        else
            pushImage ${IMAGE_NAME}
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

login

cd ${ROOT_DIR}
for IMAGE_DIR in "${IMAGE_DIRS[@]}"; do
    buildPushImage ${IMAGE_DIR}
done
