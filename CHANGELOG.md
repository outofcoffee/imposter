# Change Log

All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Added
- (openapi) Adds experimental model example generator. Enable with `--pluginArg openapi.alpha.modelexamples=true`

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
