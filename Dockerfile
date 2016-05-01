FROM java:8-jdk

MAINTAINER Pete Cornish <outofcoffee@gmail.com>

RUN mkdir -p /opt/imposter/bin /opt/imposter/config

ADD distro/build/libs/imposter.jar /opt/imposter/bin/imposter.jar

EXPOSE 8443

ENTRYPOINT ["java", "-jar", "/opt/imposter/bin/imposter.jar"]
