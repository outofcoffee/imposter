# Features

Imposter is a reliable, scriptable and extensible mock server for REST APIs, OpenAPI (and Swagger) specifications, Salesforce and HBase APIs.

Run standalone mock servers, or embed mocks within your tests (supports JVM and Node.js). Dynamic responses can be scripted using JavaScript, Groovy or Java.

## Getting started

To begin, check out our [Getting started](getting_started.md) guide. See the [User documentation](./index.md) for more.

## Highlights

* run standalone mocks in place of real systems
* turn an OpenAPI/Swagger file into a mock API for dev or QA (use it before the real API is built)
* decouple your integration tests from the cloud/back-end systems and take control of your dependencies
* validate your API requests against an OpenAPI specification
* capture data and retrieve later, or use in templates to for conditional responses

Send dynamic responses:

- Provide mock responses using static files or customise behaviour based on characteristics of the request.
- Power users can control mock responses with JavaScript or Java/Groovy script engines.
- Advanced users can write their own plugins in a JVM language of their choice.
