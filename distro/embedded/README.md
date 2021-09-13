Embedding Imposter in your tests
================================

This module enables you to embed the Imposter in your tests, using pure JVM (i.e. no Docker requirement).

Typically, you will connect your component under test to a mock HTTP endpoint provided by Imposter. You start it before your test runs, such as in your test set-up method (e.g. `@Before` in JUnit), providing your application with simulated HTTP responses, in place of a real endpoint.

## Getting started

Typically, you will want to use a plugin-specific builder if it exists, such
as `io.gatehill.imposter.openapi.embedded.OpenApiImposterBuilder` for the OpenAPI plugin.

Let's assume you have an OpenAPI specification file:

    String specFile = Paths.get("/path/to/openapi_file.yaml");

Example using an OpenAPI spec as the source:

```java
MockEngine imposter = new OpenApiImposterBuilder<>()
        .withSpecificationFile(specFile)
        .startBlocking();

// mockEndpoint will look like http://localhost:5234/v1/pets
String mockEndpoint = imposter.getBaseUrl() + "/v1/pets";

// Your component under test can interact with this endpoint to get
// simulated HTTP responses, in place of a real endpoint.
```

> For a working example, see [OpenApiImposterBuilderTest](src/test/java/io/gatehill/imposter/embedded/OpenApiImposterBuilderTest.java)

## Using a full configuration file

You can also get access to advanced Imposter features by using a standard [configuration file](../../docs/configuration.md). Pass the path to the directory containing the configuration file:

    String configDir = Paths.get("/path/to/config_dir");

Example using a directory containing an Imposter configuration file:

```java
MockEngine imposter = new ImposterBuilder<>()
        .withPluginClass(OpenApiPluginImpl.class)
        .withConfigurationDir(configDir)
        .startBlocking();

// mockEndpoint will look like http://localhost:5234/v1/pets
String mockEndpoint = imposter.getBaseUrl() + "/v1/pets";

// Your component under test can interact with this endpoint to get
// simulated HTTP responses, in place of a real endpoint.
```

> Note the need to specify the plugin, which was implicit in the example above.

> For a working example, see [ImposterBuilderTest](src/test/java/io/gatehill/imposter/embedded/ImposterBuilderTest.java)
