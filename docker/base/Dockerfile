FROM java:openjdk-8-jre

MAINTAINER Pete Cornish <outofcoffee@gmail.com>

RUN mkdir -p /opt/imposter/bin /opt/imposter/lib /opt/imposter/config

ADD ./docker/base/files/docker-entrypoint.sh /opt/imposter/bin
ADD ./distro/build/libs/imposter.jar /opt/imposter/lib

RUN ln -s /opt/imposter/bin/docker-entrypoint.sh /usr/local/bin/imposter && \
    chmod +x /usr/local/bin/imposter

EXPOSE 8443

ENTRYPOINT ["imposter"]
