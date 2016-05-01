#!/usr/bin/env bash
set -e

IMAGE_BASE_NAME="outofcoffee/imposter"
IMAGE_TAG="${1-dev}"
BUILD_FROM_SRC="${2-local}"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

function buildImage()
{
    IMAGE_NAME="${IMAGE_BASE_NAME}$1:${IMAGE_TAG}"
    echo -e "\nBuilding Docker image: ${IMAGE_NAME}"

    cd $2
    docker build --tag ${IMAGE_NAME} .
}

if [ "local" == "${BUILD_FROM_SRC}" ]; then
    echo -e "\nBuilding base image from local source"
    buildImage "" "${SCRIPT_DIR}/../"
fi

for IMAGE_DIR in $( cd ${SCRIPT_DIR} && ls ); do
    if [ -d ${SCRIPT_DIR}/${IMAGE_DIR} ]; then
        if [ "base" == ${IMAGE_DIR} ]; then
            if [ "remote" == "${BUILD_FROM_SRC}" ]; then
                echo -e "\nBuilding base image from remote source"
                buildImage "" "${SCRIPT_DIR}/base"
            fi
        else
            buildImage "-${IMAGE_DIR}" "${SCRIPT_DIR}/${IMAGE_DIR}"
        fi
    fi
done
