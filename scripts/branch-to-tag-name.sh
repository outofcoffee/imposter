#!/usr/bin/env bash
set -e

case $1 in
  master)
    IMAGE_TAG_NAME="latest"
    ;;
  develop)
    IMAGE_TAG_NAME="beta"
    ;;
esac

echo "${IMAGE_TAG_NAME}"
