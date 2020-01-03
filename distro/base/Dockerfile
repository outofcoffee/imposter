FROM openjdk:8u212-jre-alpine

LABEL MAINTAINER="Pete Cornish <outofcoffee@gmail.com>"
ENV JAVA_ARGS=""

RUN mkdir -p /opt/imposter/bin /opt/imposter/lib /opt/imposter/config

ADD ./distro/base/files/docker-entrypoint.sh /opt/imposter/bin

ONBUILD ARG DISTRO_NAME
ONBUILD ENV DISTRO_NAME="${DISTRO_NAME}"

ONBUILD ADD ./distro/${DISTRO_NAME}/build/libs/imposter-${DISTRO_NAME}.jar /opt/imposter/lib
ONBUILD RUN ln -s /opt/imposter/bin/docker-entrypoint.sh /usr/local/bin/imposter && \
            chmod +x /usr/local/bin/imposter

EXPOSE 8080 8443

ENTRYPOINT ["imposter"]
