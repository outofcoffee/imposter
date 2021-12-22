ARG BASE_IMAGE_TAG=latest

FROM outofcoffee/imposter-base:${BASE_IMAGE_TAG} as base
FROM gcr.io/distroless/java:11

LABEL MAINTAINER="Pete Cornish <outofcoffee@gmail.com>" \
      IMPOSTER_DISTRO="all"

USER 2048
COPY --from=base --chown=2048:2048 /opt/imposter /opt/imposter
COPY --from=base --chown=2048:2048 /usr/local/bin/imposter /usr/local/bin/imposter

ENV IMPOSTER_ENGINE=unpacked \
    IMPOSTER_JVM_DISTRODIR=/opt/imposter

EXPOSE 8080 8443
ENTRYPOINT ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-classpath", "/opt/imposter/lib/*", "io.gatehill.imposter.cmd.ImposterLauncher"]
CMD ["--configDir=/opt/imposter/config"]
