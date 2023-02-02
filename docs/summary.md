# Product summary

Imposter is a mock server for REST APIs, OpenAPI (and Swagger) specifications, SOAP web services (and WSDL files), Salesforce and HBase APIs.

- Run **standalone** mock servers in Docker, Kubernetes, AWS Lambda or on the JVM.
- **Embed** mocks within your tests (JVM or Node.js) to remove external dependencies.
- Script **dynamic** responses using JavaScript, Groovy or Java.
- **Capture** data from requests, then store it or return a **templated** response.
- **Proxy** an existing endpoint to replay its responses as a mock.

## Getting started

To begin, check out our [Getting started](getting_started.md) guide. See the [User documentation](./index.md) for more.

## Highlights

- run standalone mocks in place of real systems
- turn an OpenAPI/Swagger file or WSDL file into a mock API for dev or QA (use it before the real API is built)
- decouple your integration tests from the cloud/back-end systems and take control of your dependencies
- validate your API requests against an OpenAPI specification
- capture data to retrieve later, or use in templates to for conditional responses
- proxy an existing endpoint to replay its responses as a mock

Send dynamic responses:

- Provide mock responses using static files or customise behaviour based on characteristics of the request.
- Power users can control mock responses with JavaScript or Java/Groovy script engines.
- Advanced users can write their own plugins in a JVM language of their choice.

## Mock types

Imposter provides specialised mocks for the following scenarios:

- **[OpenAPI](openapi_plugin.md)** - Support for OpenAPI (and Swagger) API specifications.
- **[REST](rest_plugin.md)** - Mocks RESTful or plain HTTP APIs.
- **[SOAP](soap_plugin.md)** - Support for SOAP web services (and WSDL files).
- **[HBase](hbase_plugin.md)** - Basic HBase mock implementation.
- **[SFDC (Salesforce)](sfdc_plugin.md)** - Basic Salesforce mock implementation.
- **[WireMock](wiremock_plugin.md)** - Support for WireMock mappings files.
- 