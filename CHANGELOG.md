# Change Log

All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
- ... 

## [0.6.0] - 2017-10-14 
### Added
- Exposes request headers to scripts (thanks, kareem-habib).
- Adds withData response behaviour (thanks, yanan-l).
- Adds withHeader response behaviour (thanks, benjvoigt).
- Adds documentation for new response behaviours.
- Prints version in status response.

### Changed
- Switches to asynchronous request handling, which should improve performance under load.
- Updates swagger-ui to version 3.0.21 (thanks, benjvoigt).
- Bumps various dependency versions.

## [0.5.0] - 2016-09-03
### Added
- Adds JavaScript scripting support.

### Changed
- Refactored documentation.

## [0.4.0] - 2016-08-29
### Added
- Adds changelog (this document!)
- Adds CLI support for loading multiple plugins.
- Adds CLI and core support for specifying multiple config directories.
- Now detects plugins based on provided configuration files. This means you no longer have to specify the plugin class explicitly.
- Adds core support for specifying plugin class via META-INF properties file.

### Changed
- Breaking change - response files are now resolved relative to the plugin configuration file, not the core configuration directory.

## [0.3.4] - 2016-08-22
### Added
- Adds RecordInfo and record ID to context for HBase plugin.

## [0.3.3] - 2016-05-08
### Added
- Adds API sandbox using swagger-ui for OpenAPI plugin.

## [0.3.2] - 2016-05-04
### Added
- Adds HBase content negotiation. Supports JSON and protobuf serialisation/deserialisation.
- Allows plugins to declare additional dependency modules.

## [0.3.1] - 2016-05-02
### Added
- Adds option to serve first example found if no exact match is found in OpenAPI plugin.

## [0.3.0] - 2016-05-01
### Added
- Adds OpenAPI (aka Swagger) API specification plugin.
- Adds request method to script context for all plugins.
- Adds table name to HBase plugin.

## [0.2.5] - 2016-04-26
### Added
- Adds REST plugin support for ID field name (like HBase plugin).
- Adds REST plugin subresources to return objects or arrays of data.

## [0.2.4] - 2016-04-25
### Changed
- Using args4j for command line arguments in place of system properties.

## [0.2.3] - 2016-04-21
### Added
- Support named ID field in HBase plugin.

## [0.2.2] - 2016-04-20
### Added
- Adds the ability to get a single row from an HBase table.

## [0.2.1] - 2016-04-18
### Added
- Adds script support to other plugins.

## [0.2.0] - 2016-04-17
### Added
- Adds support for Groovy scripting of REST response behaviour.

## [0.1.0] - 2016-04-16
### Added
- Initial release.
- REST plugin.
- HBase mock plugin.
- SFDC plugin.
