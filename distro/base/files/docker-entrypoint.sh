#!/usr/bin/env bash

exec java ${JAVA_ARGS} -jar /opt/imposter/lib/imposter-${DISTRO_NAME}.jar "$@"
