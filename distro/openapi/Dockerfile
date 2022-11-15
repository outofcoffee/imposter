ARG BASE_IMAGE_TAG=latest

FROM outofcoffee/imposter-base:${BASE_IMAGE_TAG}

LABEL MAINTAINER="Pete Cornish <outofcoffee@gmail.com>"

WORKDIR /opt/imposter/config

CMD ["--plugin", "openapi", "--configDir", "/opt/imposter/config"]
