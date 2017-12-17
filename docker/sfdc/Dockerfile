ARG BASE_IMAGE_TAG=latest

FROM outofcoffee/imposter:${BASE_IMAGE_TAG}

MAINTAINER Pete Cornish <outofcoffee@gmail.com>

CMD ["--plugin", "com.gatehill.imposter.plugin.sfdc.SfdcPluginImpl", "--configDir", "/opt/imposter/config"]
