#!/usr/bin/env bash
set -e

IMAGE_BASE_NAME="outofcoffee/imposter"
IMAGE_TAG="${1-dev}"
BUILD_FROM_SRC="${2-local}"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

function pushImage()
{
    IMAGE_NAME="${IMAGE_BASE_NAME}$1:${IMAGE_TAG}"
    echo -e "\nPushing Docker image: ${IMAGE_NAME}"

    docker push ${IMAGE_NAME}
}

if [ "local" == "${BUILD_FROM_SRC}" ]; then
    echo -e "\nPushing base image built from local source"
    pushImage ""
fi

for IMAGE_DIR in $( cd ${SCRIPT_DIR} && ls ); do
    if [ -d ${SCRIPT_DIR}/${IMAGE_DIR} ]; then
        if [ "base" == ${IMAGE_DIR} ]; then
            if [ "remote" == "${BUILD_FROM_SRC}" ]; then
                echo -e "\nPushing base image built from remote source"
                pushImage ""
            fi
        else
            pushImage "-${IMAGE_DIR}"
        fi
    fi
done
