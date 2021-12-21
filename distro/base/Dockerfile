FROM adoptopenjdk/openjdk8:x86_64-alpine-jre8u312-b07

LABEL MAINTAINER="Pete Cornish <outofcoffee@gmail.com>"

RUN addgroup -S imposter && \
    adduser -S imposter -G imposter

# add CLI
ENV IMPOSTER_ENGINE=unpacked IMPOSTER_JVM_DISTRODIR=/opt/imposter
ARG IMPOSTER_CLI_VERSION="0.8.0"
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
ONBUILD ADD ./distro/${DISTRO_NAME}/build/install/imposter/bin/* /opt/imposter/bin/
ONBUILD ADD ./distro/${DISTRO_NAME}/build/install/imposter/lib/* /opt/imposter/lib/
ONBUILD RUN chmod +x /opt/imposter/bin/docker-entrypoint.sh && \
            chown -R imposter:imposter /opt/imposter

ONBUILD USER imposter
EXPOSE 8080 8443

ENTRYPOINT ["/opt/imposter/bin/docker-entrypoint.sh"]
