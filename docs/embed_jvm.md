Embedding Imposter in your JVM tests
====================================

You can embed the Imposter in your JVM tests, such as JUnit or TestNG.

Typically, Imposter starts before your tests, providing synthetic responses to your unit under test. You connect your component under test to the mock HTTP endpoint provided by Imposter.

Imposter starts before your test runs, such as in your test set-up method (e.g. `@Before` in JUnit), providing your application with simulated HTTP responses, in place of a real endpoint.

## Getting started

For the version, choose the latest release from https://github.com/outofcoffee/imposter/releases

Add the following Maven repository to your build tool:

    https://s3-eu-west-1.amazonaws.com/gatehillsoftware-maven/releases/

Add the Imposter dependency:

#### Gradle

```groovy
testImplementation "io.gatehill.imposter:distro-embedded:1.24.3"
```

#### Maven

```xml
<dependency>
    <groupId>io.gatehill.imposter</groupId>
    <artifactId>distro-embedded</artifactId>
    <version>1.24.3</version>
    <scope>test</scope>
</dependency>
```

> See the _Dependencies_ section for full examples.

### OpenAPI example

It is best to use a plugin-specific builder if it exists, such as `io.gatehill.imposter.openapi.embedded.OpenApiImposterBuilder` for the OpenAPI plugin.

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

> For a working example, see [OpenApiImposterBuilderTest](https://github.com/outofcoffee/imposter/blob/master/distro/embedded/src/test/java/io/gatehill/imposter/embedded/OpenApiImposterBuilderTest.java)

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

> For a working example, see [ImposterBuilderTest](https://github.com/outofcoffee/imposter/blob/master/distro/embedded/src/test/java/io/gatehill/imposter/embedded/ImposterBuilderTest.java)

## Dependencies

Use the following Maven dependencies in your build tool:

| Component       | Value                                                                |
|-----------------|----------------------------------------------------------------------|
| Group ID        | `io.gatehill.imposter`                                               |
| Artifact ID     | `distro-embedded`                                                    |
| Version         | As per [Releases](https://github.com/outofcoffee/imposter/releases). |

### Gradle

Using Gradle, add the following to your build configuration:

```groovy
repositories {
    maven {
        // imposter maven repository
        url 'https://s3-eu-west-1.amazonaws.com/gatehillsoftware-maven/releases/'
    }
}


dependencies {
    // choose latest release from: https://github.com/outofcoffee/imposter/releases
    testImplementation "io.gatehill.imposter:distro-embedded:1.0.0"
    
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
    <dependencies>
        <dependency>
            <groupId>io.gatehill.imposter</groupId>
            <artifactId>distro-embedded</artifactId>
            <!-- choose latest release from https://github.com/outofcoffee/imposter/releases -->
            <version>1.0.0</version>
            <scope>test</scope>
        </dependency>
        ...
    </dependencies>
</project>
```
