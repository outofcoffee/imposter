# Embedding Imposter in your application

You can embed Imposter in other applications.

To add dependencies on Imposter, using Gradle, add the following to your build configuration:

```
ext {
    // set to the desired version of imposter
    version_imposter_maven = '1.0.0'
}

repositories {
    maven {
        // imposter maven repository
        url 'https://s3-eu-west-1.amazonaws.com/gatehillsoftware-maven/releases/'
    }
}


dependencies {
    // imposter dependencies
    compile "io.gatehill.imposter:imposter-core:$version_imposter_maven"
    compile "io.gatehill.imposter:imposter-server:$version_imposter_maven"
    compile "io.gatehill.imposter:openapi:$version_imposter_maven"
```

See `io.gatehill.imposter.server.ImposterVerticle` for an example of how to bootstrap the mock server.
