#!/usr/bin/env bash
set -e

IMAGE_BASE_NAME="outofcoffee/imposter"
IMAGE_TAG="${1-dev}"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

function pushImage()
{
    IMAGE_NAME="${IMAGE_BASE_NAME}$1:${IMAGE_TAG}"
    echo "" && echo "Pushing Docker image: ${IMAGE_NAME}"

    docker push ${IMAGE_NAME}
}

for IMAGE_DIR in $( cd ${SCRIPT_DIR} && ls ); do
    if [ -d ${SCRIPT_DIR}/${IMAGE_DIR} ]; then
        if [ "base" == ${IMAGE_DIR} ]; then
            pushImage ""
        else
            pushImage "-${IMAGE_DIR}"
        fi
    fi
done
