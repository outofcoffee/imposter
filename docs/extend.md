# Extending Imposter in a custom application

You can extend Imposter to build a custom JVM application.

See `io.gatehill.imposter.server.ImposterVerticle` for an example of how to bootstrap the mock server.

## Dependencies

Add the Imposter dependencies to your build tool.

### Gradle

To add dependencies on Imposter, using Gradle, add the following to your build configuration:

```
ext {
    // choose latest release from: https://github.com/outofcoffee/imposter/releases
    imposter_version = '1.0.0'
}

repositories {
    maven {
        // imposter maven repository
        url 'https://s3-eu-west-1.amazonaws.com/gatehillsoftware-maven/releases/'
    }
}


dependencies {
    // core imposter dependencies
    implementation "io.gatehill.imposter:core:imposter-engine:$imposter_version"
    implementation "io.gatehill.imposter:imposter-server:$imposter_version"
    
    // specific plugins
    implementation "io.gatehill.imposter:mock-openapi:$imposter_version"
    
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
        <imposter.version>1.0.0</imposter.version>
    </properties>
    ...
    <dependencies>
        <dependency>
            <groupId>io.gatehill.imposter</groupId>
            <artifactId>imposter-core</artifactId>
            <version>${imposter.version}</version>
        </dependency>
        <dependency>
            <groupId>io.gatehill.imposter</groupId>
            <artifactId>imposter-server</artifactId>
            <version>${imposter.version}</version>
        </dependency>
        <dependency>
            <groupId>io.gatehill.imposter</groupId>
            <artifactId>mock-openapi</artifactId>
            <version>${imposter.version}</version>
        </dependency>
        ...
    </dependencies>
</project>
```
