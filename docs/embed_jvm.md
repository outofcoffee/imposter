# Embedding Imposter in your JVM tests

You can embed the Imposter in your JVM tests, such as JUnit or TestNG.

Typically, Imposter starts before your tests, providing synthetic responses to your unit under test. You connect your component under test to the mock HTTP endpoint provided by Imposter.

Imposter starts before your test runs, such as in your test set-up method (e.g. `@Before` in JUnit), providing your application with simulated HTTP responses, in place of a real endpoint.

## Getting started

> For a full working example project, see [examples/junit-sample](https://github.com/outofcoffee/imposter/tree/main/examples/junit-sample)

First, note the latest [release version](https://github.com/outofcoffee/imposter/releases).

Add the following Maven repository to your build tool:

    https://s3-eu-west-1.amazonaws.com/gatehillsoftware-maven/releases

Add the following Maven dependencies in your build tool:

| Component      | Group ID               | Artifact ID       | Version                                                                           |
|----------------|------------------------|-------------------|-----------------------------------------------------------------------------------|
| Main library   | `io.gatehill.imposter` | `distro-embedded` | As per [Releases](https://github.com/outofcoffee/imposter/releases), e.g. `4.2.2` |
| HTTP server    | `io.gatehill.imposter` | `imposter-server` | As per [Releases](https://github.com/outofcoffee/imposter/releases), e.g. `4.2.2` |
| Config parser  | `io.gatehill.imposter` | `config-dynamic`  | As per [Releases](https://github.com/outofcoffee/imposter/releases), e.g. `4.2.2` |
| OpenAPI plugin | `io.gatehill.imposter` | `mock-openapi`    | As per [Releases](https://github.com/outofcoffee/imposter/releases), e.g. `4.2.2` |

> See the _Dependencies_ section for full Maven and Gradle examples.

### OpenAPI example

It is best to use a plugin-specific builder if it exists, such as `io.gatehill.imposter.openapi.embedded.OpenApiImposterBuilder` for the OpenAPI plugin.

Let's assume you have an OpenAPI specification file:

    Path specFile = Paths.get("/path/to/openapi_file.yaml");

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

> For a full working example project, see [examples/junit-sample](https://github.com/outofcoffee/imposter/tree/main/examples/junit-sample)

## Using a full configuration file

You can also get access to advanced Imposter features by using a standard [configuration file](./configuration.md). Pass the path to the directory containing the configuration file:

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

## Dependencies

Build tool configuration for Gradle and Maven.

### Gradle

Using Gradle, add the following to your build configuration:

```groovy
ext {
    // choose latest release from: https://github.com/outofcoffee/imposter/releases
    imposter_version = '4.2.2'
}

repositories {
    maven {
        // imposter maven repository
        url 'https://s3-eu-west-1.amazonaws.com/gatehillsoftware-maven/releases/'
    }
}

dependencies {
    testImplementation "io.gatehill.imposter:distro-embedded:$imposter_version"
    testImplementation "io.gatehill.imposter:imposter-server:$imposter_version"
    testImplementation "io.gatehill.imposter:config-dynamic:$imposter_version"
    testImplementation "io.gatehill.imposter:mock-openapi:$imposter_version"
    
    // ...
}
```

### Maven

Using Maven, add the following to your POM:

```xml
<project>
    ...
    <repositories>
        <repository>
            <id>imposter</id>
            <url>https://s3-eu-west-1.amazonaws.com/gatehillsoftware-maven/releases</url>
        </repository>
    </repositories>
    ...
    <properties>
        <!-- choose latest release from https://github.com/outofcoffee/imposter/releases -->
        <imposter.version>4.2.2</imposter.version>
    </properties>
    ...
    <dependencies>
        <dependency>
            <groupId>io.gatehill.imposter</groupId>
            <artifactId>distro-embedded</artifactId>
            <version>${imposter.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.gatehill.imposter</groupId>
            <artifactId>imposter-server</artifactId>
            <version>${imposter.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.gatehill.imposter</groupId>
            <artifactId>config-dynamic</artifactId>
            <version>${imposter.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.gatehill.imposter</groupId>
            <artifactId>mock-openapi</artifactId>
            <version>${imposter.version}</version>
            <scope>test</scope>
        </dependency>
        ...
    </dependencies>
</project>
```

## Other plugins

Imposter supports a range of plugins, such as SOAP/WSDL and RESTful services. Here are some examples:

| Component       | Group ID               | Artifact ID | More information                    |
|-----------------|------------------------|-------------|-------------------------------------|
| SOAP/WSDL mocks | `io.gatehill.imposter` | `mock-soap` | See [SOAP plugin](./soap_plugin.md) |
| RESTful mocks   | `io.gatehill.imposter` | `mock-rest` | See [REST plugin](./rest_plugin.md) |

See the [Plugins](./plugins.md) section for more.
