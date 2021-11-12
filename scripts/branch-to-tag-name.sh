#!/usr/bin/env bash
set -e

case $1 in
  main)
    IMAGE_TAG_NAME="latest"
    ;;
  develop)
    IMAGE_TAG_NAME="beta"
    ;;
esac

echo "${IMAGE_TAG_NAME}"
