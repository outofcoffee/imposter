#!/usr/bin/env bash
set -e

IMAGE_BASE_NAME="outofcoffee/imposter"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

function buildImage()
{
    IMAGE_NAME="${IMAGE_BASE_NAME}$2"
    echo "" && echo "Building Docker image: ${IMAGE_NAME}"

    cd ${SCRIPT_DIR}/$1
    docker build --tag ${IMAGE_NAME} .
}

for IMAGE_DIR in $( cd ${SCRIPT_DIR} && ls ); do
    if [ -d ${SCRIPT_DIR}/${IMAGE_DIR} ]; then
        if [ "base" == ${IMAGE_DIR} ]; then
            buildImage "base" ""
        else
            buildImage ${IMAGE_DIR} "-${IMAGE_DIR}"
        fi
    fi
done
