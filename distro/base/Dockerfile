FROM eclipse-temurin:11.0.23_9-jre-jammy

LABEL MAINTAINER="Pete Cornish <outofcoffee@gmail.com>"

RUN addgroup imposter --gid 2048 && \
    adduser imposter --uid 2048 --gid 2048

# add CLI
ARG IMPOSTER_VERSION
ENV IMPOSTER_VERSION="${IMPOSTER_VERSION}" \
    IMPOSTER_ENGINE=unpacked \
    IMPOSTER_JVM_DISTRODIR=/opt/imposter

ARG IMPOSTER_CLI_VERSION="0.42.2"
ADD https://github.com/gatehill/imposter-cli/releases/download/v${IMPOSTER_CLI_VERSION}/imposter_${IMPOSTER_CLI_VERSION}_linux_amd64.tar.gz /tmp/imposter-cli/imposter.tar.gz

RUN cd /tmp/imposter-cli && \
    tar xvf /tmp/imposter-cli/imposter.tar.gz && \
    mv /tmp/imposter-cli/imposter /usr/local/bin/imposter && \
    rm -rf /tmp/imposter-cli

# add distro
RUN mkdir -p \
    /opt/imposter/bin \
    /opt/imposter/lib \
    /opt/imposter/config \
    /opt/imposter/plugins

ONBUILD ARG DISTRO_NAME
ONBUILD ENV DISTRO_NAME="${DISTRO_NAME}"
ONBUILD ADD ./distro/${DISTRO_NAME}/build/install/imposter/lib/* /opt/imposter/lib/
ONBUILD RUN chown -R imposter:imposter /opt/imposter && \
            rm -rf /bin/*

ONBUILD USER imposter

ENV IMPOSTER_PLUGIN_DIR=/opt/imposter/plugins

EXPOSE 8080 8443
ENTRYPOINT ["java", "-classpath", "/opt/imposter/lib/*", "io.gatehill.imposter.cmd.ImposterLauncher"]
