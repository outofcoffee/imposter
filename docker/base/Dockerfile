FROM java:openjdk-8-jre

MAINTAINER Pete Cornish <outofcoffee@gmail.com>

ENV IMPOSTER_TEMP_DIR=/tmp/imposter
ENV IMPOSTER_SRC_ARCHIVE=${IMPOSTER_TEMP_DIR}/imposter-src.zip
ENV IMPOSTER_SRC_DIR=${IMPOSTER_TEMP_DIR}/imposter-master

RUN mkdir -p ${IMPOSTER_TEMP_DIR} /opt/imposter/config

# build distribution from source
ADD https://github.com/outofcoffee/imposter/archive/master.zip ${IMPOSTER_SRC_ARCHIVE}
RUN unzip ${IMPOSTER_SRC_ARCHIVE} -d ${IMPOSTER_TEMP_DIR}
RUN cd ${IMPOSTER_SRC_DIR} && ./gradlew clean shadowJar
RUN mv ${IMPOSTER_SRC_DIR}/distro/build/libs/imposter.jar /opt/imposter

# cleanup
RUN rm -rf ${IMPOSTER_TEMP_DIR}
RUN rm -rf $HOME/.gradle
RUN rm -rf $HOME/.m2

EXPOSE 8443

ENTRYPOINT ["java", "-jar", "/opt/imposter/bin/imposter.jar"]
