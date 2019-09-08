ARG BASE_IMAGE_TAG=latest

FROM outofcoffee/imposter-base:${BASE_IMAGE_TAG}

LABEL MAINTAINER="Pete Cornish <outofcoffee@gmail.com>"

CMD ["--plugin", "com.gatehill.imposter.plugin.openapi.OpenApiPluginImpl", "--configDir", "/opt/imposter/config"]
