FROM alpine

LABEL MAINTAINER="Pete Cornish <outofcoffee@gmail.com>"

# add CLI
ONBUILD ARG IMPOSTER_VERSION
ONBUILD ENV IMPOSTER_VERSION="${IMPOSTER_VERSION}" \
    IMPOSTER_ENGINE=unpacked \
    IMPOSTER_JVM_DISTRODIR=/opt/imposter

ARG IMPOSTER_CLI_VERSION="0.12.7"
ADD https://github.com/gatehill/imposter-cli/releases/download/v${IMPOSTER_CLI_VERSION}/imposter_${IMPOSTER_CLI_VERSION}_Linux_x86_64.tar.gz /tmp/imposter-cli/imposter.tar.gz

RUN cd /tmp/imposter-cli && \
    tar xvf /tmp/imposter-cli/imposter.tar.gz && \
    mv /tmp/imposter-cli/imposter /usr/local/bin/imposter && \
    chmod 744 /usr/local/bin/imposter

# add distro
RUN mkdir -p \
    /opt/imposter/bin \
    /opt/imposter/lib \
    /opt/imposter/config \
    /opt/imposter/plugins

ONBUILD ARG DISTRO_NAME
ONBUILD ADD ./distro/${DISTRO_NAME}/build/install/imposter/lib/* /opt/imposter/lib/

ONBUILD ENV JAVA_TOOL_OPTIONS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
ONBUILD EXPOSE 8080 8443

ONBUILD ENTRYPOINT ["java", "-classpath", "/opt/imposter/lib/*", "io.gatehill.imposter.cmd.ImposterLauncher"]
