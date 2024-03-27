FROM outofcoffee/imposter as imposter

FROM eclipse-temurin:11.0.22_7-jre-jammy
COPY --from=imposter /opt/imposter/lib /opt/imposter/lib

# your custom config
COPY config /opt/imposter/config

# optional docker healthcheck
HEALTHCHECK --interval=5s --timeout=5s --start-period=5s --retries=3 CMD curl -f http://localhost:8080/system/status || exit 1

ENTRYPOINT ["java", "-classpath", "/opt/imposter/lib/*", "io.gatehill.imposter.cmd.ImposterLauncher", "--configDir", "/opt/imposter/config"]
