FROM anapsix/alpine-java:jdk8

MAINTAINER Pete Cornish <outofcoffee@gmail.com>

ENV IMPOSTER_TEMP_DIR=/tmp/imposter
ENV IMPOSTER_SRC_ARCHIVE=${IMPOSTER_TEMP_DIR}/imposter-src.zip
ENV IMPOSTER_SRC_DIR=${IMPOSTER_TEMP_DIR}/imposter-master

RUN mkdir -p ${IMPOSTER_TEMP_DIR} /opt/imposter/config

ADD https://github.com/outofcoffee/imposter/archive/master.zip ${IMPOSTER_SRC_ARCHIVE}

RUN unzip ${IMPOSTER_SRC_ARCHIVE} -d ${IMPOSTER_TEMP_DIR}

RUN cd ${IMPOSTER_SRC_DIR} && ./gradlew clean shadowJar

RUN mv ${IMPOSTER_SRC_DIR}/distro/build/libs/imposter.jar /opt/imposter

EXPOSE 8443

ENTRYPOINT ["java", "-jar", "/opt/imposter/imposter.jar"]
