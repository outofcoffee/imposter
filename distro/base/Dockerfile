FROM adoptopenjdk/openjdk8:x86_64-alpine-jre8u312-b07

LABEL MAINTAINER="Pete Cornish <outofcoffee@gmail.com>"
ENV JAVA_ARGS=""

# CLI support
ENV IMPOSTER_ENGINE=jvm \
    IMPOSTER_JVM_JARFILE=/opt/imposter/lib/imposter.jar

ARG IMPOSTER_CLI_VERSION="0.7.3"
ADD https://github.com/gatehill/imposter-cli/releases/download/v${IMPOSTER_CLI_VERSION}/imposter_${IMPOSTER_CLI_VERSION}_Linux_x86_64.tar.gz /tmp/imposter-cli/imposter.tar.gz

RUN cd /tmp/imposter-cli && \
    tar xvf /tmp/imposter-cli/imposter.tar.gz && \
    mv /tmp/imposter-cli/imposter /usr/local/bin/imposter && \
    rm -rf /tmp/imposter-cli

# add distro
RUN mkdir -p /opt/imposter/bin /opt/imposter/lib /opt/imposter/config
COPY ./distro/base/files/docker-entrypoint.sh /opt/imposter/bin

ONBUILD ARG DISTRO_NAME
ONBUILD ENV DISTRO_NAME="${DISTRO_NAME}"
ONBUILD ADD ./distro/${DISTRO_NAME}/build/libs/imposter-${DISTRO_NAME}.jar /opt/imposter/lib/imposter.jar
ONBUILD RUN chmod +x /opt/imposter/bin/docker-entrypoint.sh

EXPOSE 8080 8443

ENTRYPOINT ["/opt/imposter/bin/docker-entrypoint.sh"]
