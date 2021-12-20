# Change Log

All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [2.4.6] - 2021-12-20
### Fixed
- fix: bumps log4j2 to 2.17.0 to mitigate CVE-2021-45105.

## [2.4.5] - 2021-12-17
### Changed
- refactor: switches boot to direct invocation of unpacked wrapper script.

## [2.4.4] - 2021-12-17
### Changed
- chore: bumps CLI version to 0.7.7.

### Fixed
- fix: resolves placeholders when preparsing configuration files.

## [2.4.3] - 2021-12-17
### Fixed
- fix: version should be reported by status endpoint.

## [2.4.2] - 2021-12-17
### Added
- feat: prints summary on underlying adapter 404 responses.

## [2.4.1] - 2021-12-16
### Added
- feat: allows individual capture configuration to be enabled/disabled.

## [2.4.0] - 2021-12-16
### Added
- feat: adds structured logging with durations, request and response properties.

## [2.3.2] - 2021-12-16
### Fixed
- fix: bumps log4j2 to 2.16.0 to mitigate CVE-2021-45046.

## [2.3.1] - 2021-12-15
### Changed
- feat: switches boot to CLI unpacked method.
- build: improves JAR naming for non-distribution archives.

## [2.3.0] - 2021-12-13
### Added
- feat: adds DynamoDB store implementation.
- feat: adds DynamoDB store support to Lambda distro.
- feat: implements store-wide deletion in Redis and DynamoDB stores.

### Fixed
- fix: uses host header if available for Lambda adapter base URL.
- fix: captures Lambda path parameters explicitly instead of relying on request event.

## [2.2.3] - 2021-12-12
### Fixed
- fix: disable unneeded file upload directory and interception in Vert.x Web. 

## [2.2.2] - 2021-12-12
### Changed
- chore: bumps JRE base image to 8u312-b07.

## [2.2.1] - 2021-12-11
### Fixed
- fix: bumps log4j2 to 2.15.0 to mitigate CVE-2021-44228.

## [2.2.0] - 2021-12-02
### Added
- feat: adds multiple plugin support to embedded distro.
- feat: allows advanced customisation of engine settings in embedded distro.
- feat: adds Lambda adapter and distro.
- docs: adds Lambda example.

### Changed
- refactor: moves vertx-web adapter implementation and dependency into separate module.
- refactor: moves api, config, engine and S3 config resolver modules under core.

### Fixed
- fix: correctly serialises security schemes in OpenAPI plugin.
- fix: resolves some compiler warnings.

## [2.1.0] - 2021-11-23
### Added
- feat: adds a server entry to the combined OpenAPI spec for the mock endpoint.
- feat: adds support for CLI boot. CLI boot is currently experimental, but supports hot-reload of configuration and other [CLI features](https://github.com/gatehill/imposter-cli).

## [2.0.2] - 2021-11-17
### Fixed
- fix: resolves some compiler warnings.
- fix: initialises scripts once even if referenced multiple times.
- fix: restores behaviour of deprecated params property.

## [2.0.1] - 2021-11-15
### Changed
- feat: precompiles JavaScript scripts on startup to lower initial request latency.

### Fixed
- fix: improves error trapping when dynamic item name cannot be resolved.

## [2.0.0] - 2021-11-12
### Changed
- refactor: ports codebase to Kotlin.

### Fixed
- fix: stops path parameters being treated as query parameters during OpenAPI request validation.
- fix: stops path parameters being treated as query parameters during condition and resource matching.

## [1.24.5] - 2021-11-12
### Added
- (docs) Links to example JUnit project.

### Changed
- (build) Bumps Jackson to 2.12.5.

## [1.24.4] - 2021-10-29
### Added
- (docs) Adds JUnit sample project.
- (ci) Publishes base distro Maven artifacts.

### Changed
- (openapi) Logs full URI to OpenAPI specification UI at startup. 
- (core) Quietens logging of unhandled errors for favicons to TRACE. 
- (build) Upgrades Gradle to 6.8.1.
- (build) Increases granularity of module dependency scopes.

## [1.24.3] - 2021-10-28
### Added
- (ci) Publishes embedded distro Maven artifacts.

## [1.24.2] - 2021-10-26
### Added
- (ci) Uploads JAR release asset without version suffix.

## [1.24.1] - 2021-10-26
### Added
- (core) Adds JS type support for stores.

## [1.24.0] - 2021-10-21
### Added
- (core) Allows header keys to be forced to lowercase.

## [1.23.3] - 2021-10-06
### Changed
- (core) Quietens logs for core engine startup.

## [1.23.2] - 2021-10-01
### Added
- (core) Adds support for scripts using `@imposter-js/types`. See https://github.com/gatehill/imposter-js-types

### Changed
- (core) Caches script logger for improved lookup performance.

## [1.23.1] - 2021-09-28
### Changed
- (ci) Switches CI to GitHub Actions.

## [1.23.0] - 2021-09-18
### Added
- (core) Adds support for dynamic capture names.
- (core) Adds support for constant capture values.
- (docs) Adds preloading documentation.

## [1.22.0] - 2021-09-17
### Added
- (all) Applies license.

## [1.21.1] - 2021-09-16
### Added
- (core) Adds embedded JVM distribution. This allows use of Imposter in JUnit tests. See [JVM bindings](./docs/embed_jvm.md).
- (docs) Adds links to [Imposter CLI](./docs/run_imposter_cli.md) and [JavaScript bindings](https://github.com/gatehill/imposter-js).

## [1.21.0] - 2021-09-13
### Added
- (openapi) Allows OpenAPI specification to be loaded from a URL.
- (openapi) Allows OpenAPI specification to be loaded from an S3 bucket.
- (core) Adds support for preloading stores from file or inline data.
- (core) Adds store item count and allows retrieval of object items from store REST API.
- (core) Allows inline data to be used as a response template.

### Changed
- (openapi) Sets response content type from OpenAPI mime type.

## [1.20.0] - 2021-08-31
### Added
- (core) Allows response file cache size to be configured.
- (core) Improves request ID logging in exception handlers.
- (core) Adds metric gauge for script cache.
- (core) Adds metric gauge for response file cache.
- (core) Adds metric timer for script execution duration.

### Changed
- (core) Disables ordered execution, as responses should not block each other. This provides up to a 2x throughput improvement under load.
- (core) Increases default response file cache size to 20.

## [1.19.0] - 2021-08-31
### Added
- (core) Adds support for OpenAPI style path parameter syntax.
- (rest) Adds REST plugin resource support for matching path parameters, query parameters and request headers.

### Changed
- (core) Bumps base JRE image to 8u302.

## [1.18.1] - 2021-08-23
### Added
- (openapi, rest) Allows root response to be used as defaults for resource responses.

### Changed
- (core) Moves request scoped store cleanup to after routing context handler completes.

### Fixed
- (openapi) Fixes root resource being used instead of active resource when sending OpenAPI plugin response.
- (core) Determines content type for response files in the same way regardless of whether template is used.

## [1.18.0] - 2021-08-19
### Added
- (openapi, rest) Allows resource matching against request body using JsonPath. See [request matching](./docs/request_matching.md).
- (docs) Improves documentation for data capture, response templates and advanced request matching.

### Fixed
- (core) Fixes JSON formatting of status response.

## [1.17.1] - 2021-08-16
### Fixed
- (core) Forces header key insensitivity when evaluating security conditions.

## [1.17.0] - 2021-08-11
### Added
- (core) Improves detection of missing configuration files
- (core) Allows store item lookup in response templates.
- (core) Enables capture of path parameters, query parameters and request headers for resources.
- (core) Enables capture of request body properties using JsonPath.
- (core) Enables response template interpolation using JsonPath.
- (core) Adds request scoped store.
- (core) Adds server response header.
- (core) Enables JVM metrics collection.
- (core) Improves trapping and request ID logging for unhandled errors.

### Fixed
- (openapi) Fixes OpenAPI spec path redirect.

## [1.16.0] - 2021-08-09
### Added
- (openapi) Allows resource matching based on request headers.
- (openapi) Adds log-only option for OpenAPI validation.
- (openapi) Allows configuration of default OpenAPI validation issue behaviour.

## [1.15.1] - 2021-07-30
### Added
- (core) Adds normalised headers map to script context request. This aids script portability, avoiding case-sensitivity for header keys.

### Changed
- (core) Bumps base JRE image to 8u292.

## [1.15.0] - 2021-07-21
### Added
- (core) Graduates Stores to GA.
- (docs) Improves metrics examples.

### Changed
- (openapi) Enables full resolution of OpenAPI references.

### Fixed
- (openapi) Supports example references in OpenAPI specifications.

## [1.14.1] - 2021-07-06
### Changed
- (core) Switches in memory store implementation to concurrent map.

## [1.14.0] - 2021-07-05
### Added
- (core) Adds Redis store implementation.
- (core) Adds support for store key prefixes.

### Changed
- (core) Improves script execution time logging and URI logging.

## [1.13.0] - 2021-06-28
### Added
- (core) Adds experimental Stores support.

## [1.12.0] - 2021-06-23
### Added
- (core) Allows configuration driven and script driven latency simulation.
- (core) Adds environment variables to runtime context.

### Changed
- (core) Deprecates confusing DSL method 'immediately()' in favour of a more descriptive name 'skipDefaultBehaviour()'. This is a non-breaking change, but will be removed in a future major release.

## [1.11.0] - 2021-06-22
### Added
- (core) Allows log level to be set using environment variable.
- (core) Exposes Prometheus metrics endpoint.

### Changed
- (core) Bumps Vert.x version to 3.7.1.
- (core) Compiles and caches scripts in the Nashorn script engine, substantially improving execution speed.

## [1.10.1] - 2021-06-11
### Fixed
- (core) Matches security policy headers in a case-insensitive manner.

## [1.10.0] - 2021-06-09
### Added
- (openapi, rest) Adds security policy support to resources.

### Changed
- (openapi) Bumps Swagger UI version to 3.50.0.

## [1.9.0] - 2021-06-07
### Added
- (core) Adds path parameter support to declarative config and script engines.
- (core) Adds query parameter support to security policy condition.

### Changed
- (core) Renames params to queryParams in configuration and script engine.

## [1.8.0] - 2021-06-05
### Added
- (core) Allows request security to be controlled via plugin configuration. This supports authentication using request header values.
- (core) Allows substitution of environment variables in plugin configuration.

## [1.7.0] - 2021-05-25
### Added
- (openapi) Allows matching of response behaviours based on request parameters in static configuration.
- (openapi) Allows specification of example name in static configuration.
- (core) Exposes request path to scripts for easier conditional logic.

## [1.6.2] - 2021-05-07
### Fixed
- (core) Allows request parameters to be accessed using Nashorn and Graal.js script engines.

## [1.6.1] - 2021-05-06
### Fixed
- (openapi) Correctly validates request query parameters.

## [1.6.0] - 2021-05-05
### Added
- (openapi) Allows OpenAPI validation levels to be configured.
- (openapi) Adds format-aware default value generators.
- (docs) Improves OpenAPI plugin documentation.

### Fixed
- (openapi) Fixes example collection for date and date-time schemas (thanks, zellerr).

## [1.5.0] - 2021-05-03
### Added
- (openapi) Allows request validation to be enabled via OpenAPI plugin configuration.

### Changed
- (openapi) OpenAPI plugin returns first status code for operation if none set explicitly.
- (openapi) Returns the first value of an enum when building an example from an OpenAPI schema.

## [1.4.0] - 2021-04-29
### Added
- (openapi) Enables default status codes to be defined for OpenAPI paths.
- (openapi) Adds path specific response behaviours to OpenAPI plugin.

## [1.3.0] - 2021-04-20
### Added
- (openapi) Enables response examples to be selected by name.
- (core) Adds GraalVM script engine as Nashorn is deprecated from Java 11. Nashorn is still the default.
- (docs) Improves scripting documentation.

## [1.2.0] - 2020-04-30
### Added
- (openapi) Adds support for response refs.
- (openapi) Enables schema model ref lookup by default.
- (openapi) Improves array example serialisation.
- (openapi) Improves test coverage for object and schema examples, including YAML serialisations.

## [1.1.2] - 2020-02-22
### Added
- (openapi) Adds experimental model example generator.
- (openapi) Improves base path handling to remove double slashes at the start of full paths.

## [1.1.1] - 2019-11-25
### Added
- (openapi, rest) Allows static responses to specify headers and inline data.
- (rest) Allows HTTP method to be set on root REST plugin resource.
- (docs) Improves configuration documentation.

## [1.1.0] - 2019-11-25
### Added
- (core) Enables plugins to be specified by their short name.
- (docs) Improves examples and documentation.

### Changed
- (openapi) Sets OpenAPI base path on server URI instead of path.
- (openapi) Bumps Swagger UI to 3.24.3.
- (rest) Makes configuration enum deserialisation case insensitive.
- (rest) Sets default REST resource type to object.

## [1.0.1] - 2019-11-17
### Added
- (core) Adds support for YAML-formatted configuration files.
- (rest) Improves REST plugin examples and enables non-string ID comparision.

### Fixed
- (openapi) Improves OpenAPI path parameter detection.
- (openapi) Improves error trapping when overriding OpenAPI scheme.

## [1.0.0] - 2019-10-20
### Added
- (openapi) Adds OpenAPI v3 support. This means the OpenAPI plugin now supports both Swagger/OpenAPI v2 and OpenAPI v3 files.
- (openapi) Adds support for object response examples in the OpenAPI plugin.

### Changed
- (core) Breaking change: Defaults HTTP listen port to 8080.
- (core) Slims distributions to include only the required dependencies.
- (core) Switches Docker base image to Alpine.
- (core) Enables cgroup-aware heap sizing JVM option.

### Fixed
- (core) Automatic plugin detection now works properly.

## [0.7.0] - 2019-08-03
### Added
- (core) Adds extension point to allow custom HTTP server implementation.
- (core) More modules published for extension developers.

### Fixed
- (core) Verticle startup no longer blocks the main thread.
- (hbase) HBase plugin tests now included in test reports.

### Changed
- (core) Moves core API into Maven-published 'api' module.
- (sfdc) Sets SFDC plugin content type to JSON (thanks, pauturner).

## [0.6.0] - 2017-10-14
### Added
- (core) Exposes request headers to scripts (thanks, kareem-habib).
- (core) Adds withData response behaviour (thanks, yanan-l).
- (core) Adds withHeader response behaviour (thanks, benjvoigt).
- (docs) Adds documentation for new response behaviours.
- (core) Prints version in status response.

### Changed
- (core) Switches to asynchronous request handling, which should improve performance under load.
- (openapi) Updates swagger-ui to version 3.0.21 (thanks, benjvoigt).
- (core) Bumps various dependency versions.

## [0.5.0] - 2016-09-03
### Added
- (core) Adds JavaScript scripting support.

### Changed
- (docs) Refactored documentation.

## [0.4.0] - 2016-08-29
### Added
- (docs) Adds changelog (this document!)
- (core) Adds CLI support for loading multiple plugins.
- (core) Adds CLI and core support for specifying multiple config directories.
- (core) Now detects plugins based on provided configuration files. This means you no longer have to specify the plugin class explicitly.
- (core) Adds core support for specifying plugin class via META-INF properties file.

### Changed
- (core) Breaking change: response files are now resolved relative to the plugin configuration file, not the core configuration directory.

## [0.3.4] - 2016-08-22
### Added
- (hbase) Adds RecordInfo and record ID to context for HBase plugin.

## [0.3.3] - 2016-05-08
### Added
- (openapi) Adds API sandbox using swagger-ui for OpenAPI plugin.

## [0.3.2] - 2016-05-04
### Added
- (hbase) Adds HBase content negotiation. Supports JSON and protobuf serialisation/deserialisation.
- (core) Allows plugins to declare additional dependency modules.

## [0.3.1] - 2016-05-02
### Added
- (openapi) Adds option to serve first example found if no exact match is found in OpenAPI plugin.

## [0.3.0] - 2016-05-01
### Added
- (openapi) Adds OpenAPI (aka Swagger) API specification plugin.
- (core) Adds request method to script context for all plugins.
- (hbase) Adds table name to HBase plugin.

## [0.2.5] - 2016-04-26
### Added
- (rest) Adds REST plugin support for ID field name (like HBase plugin).
- (rest) Adds REST plugin subresources to return objects or arrays of data.

## [0.2.4] - 2016-04-25
### Changed
- (core) Using args4j for command line arguments in place of system properties.

## [0.2.3] - 2016-04-21
### Added
- (hbase) Support named ID field in HBase plugin.

## [0.2.2] - 2016-04-20
### Added
- (hbase) Adds the ability to get a single row from an HBase table.

## [0.2.1] - 2016-04-18
### Added
- (hbase, sfdc) Adds script support to other plugins.

## [0.2.0] - 2016-04-17
### Added
- (rest) Adds support for Groovy scripting of REST response behaviour.

## [0.1.0] - 2016-04-16
### Added
- Initial release.
- REST plugin.
- HBase mock plugin.
- SFDC plugin.
