# Change Log

All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [3.44.0] - 2024-06-29
### Added
- feat(soap): adds support for document wrapped style.
- feat(soap): includes parent config XML namespaces when using allOf and anyOf body matchers.

### Changed
- ci: improves job names.
- ci: improves workflow names.
- docs: updates CI badge URL.

## [3.43.5] - 2024-06-26
### Changed
- ci: determine version using since.
- ci: sets fetch depth on CI push.
- ci: sets since log level to info.
- ci: sets since version for download.
- ci: takes version as input.

## [3.43.4] - 2024-06-26
### Changed
- ci: passes through release input.
- ci: release cut uses main effective branch.

## [3.43.3] - 2024-06-26
### Changed
- ci: allows release cut workflow to write checks.
- ci: factors out CI workflow.

## [3.43.2] - 2024-06-26
### Changed
- ci: adds release workflow.
- ci: fetch tags on release checkout.
- ci: sets fetch depth on release checkout.

## [3.43.1] - 2024-06-26
### Changed
- ci: makes release job conditional on beta or release tag.
- ci: splits integration test from validate job.
- ci: splits maven publish step from release job.
- docs: simplifies scripting documentation.

## [3.43.0] - 2024-06-26
### Added
- feat(openapi): adds support for interceptors to openapi plugin.
- feat(rest): adds support for interceptors to rest plugin.
- feat: adds support for interceptors.

### Changed
- chore(deps): bump org.apache.maven.plugins:maven-clean-plugin
- docs: adds examples for interceptors.
- docs: adds interceptors documentation.
- docs: fixes links to stores documentation.
- docs: fixes script benchmarks based on current version.
- docs: fixes top level file comments.
- docs: updates benchmarks based on current version.
- refactor: replaces Vert.x blocking executor with coroutines.
- refactor: replaces future handlers with coroutine await.
- refactor: run interceptors in a coroutine.
- refactor: short-circuited responses should respect response behaviour.

## [3.42.1] - 2024-06-25
### Changed
- ci: fix release condition.

## [3.42.0] - 2024-06-25
### Added
- feat: adds distroless Docker image.

### Changed
- build(docker): disable parallel builds in buildkit.
- build(docker): uses gradle base image for distroless build stage.
- build: defaults docker build convenience script to not push images.
- build: don't run shadowjar in distroless image build.
- build: moves Docker ignore files next to Dockerfiles.
- build: moves docker build image name to script arg.
- build: switches distroless tag to nonroot.
- chore(deps): bump org.apache.maven.plugins:maven-jar-plugin
- chore(deps): bump org.apache.maven.plugins:maven-project-info-reports-plugin
- chore(deps): bump org.apache.maven.plugins:maven-surefire-plugin
- chore: bumps base JRE to 11.0.23_9.
- ci: don't duplicate unit tests when building distributions.
- ci: enables dist workflow for PR builds.
- ci: indent buildkitd config file.
- ci: removes unneeded matrix strategy from dist workflow.
- ci: splits out distroless image build into separate job.
- ci: splits out validate and dist steps into separate jobs.
- ci: switches distroless to external build.
- ci: work-around for https://github.com/actions/runner/issues/2206
- docs: improves description of environment variable purpose.
- refactor: replaces legacy internal colon-delimited path params with bracketed format.

### Fixed
- fix: adds option to escape non-param colons in path.

## [3.41.2] - 2024-06-09
### Changed
- chore: bumps CLI to 0.42.3.

## [3.41.1] - 2024-06-09
### Changed
- chore: bumps CLI to 0.42.2.

## [3.41.0] - 2024-06-09
### Added
- feat(groovy): adds support for Groovy inline script steps.
- feat: switches to bracketed parameter format internally.

### Changed
- docs: prefers bracketed path parameter format.
- test: fixes JUnit build config for vertx-web.

## [3.40.0] - 2024-05-19
### Added
- feat(graal): adds optional store proxy.

### Changed
- refactor(graal): uses inbuilt map proxy for request properties.
- refactor: adds Guice binding asSingleton() extension function.
- test: adds hamcrest to test-utils.

## [3.39.1] - 2024-05-18
### Changed
- chore(deps): bump com.fasterxml.jackson.core:jackson-databind
- chore(deps): bump org.apache.maven.plugins:maven-deploy-plugin
- chore(deps): bump org.apache.maven.plugins:maven-install-plugin
- chore(deps): bump version_groovy from 4.0.13 to 4.0.21
- docs: improves style of scripting example.
- refactor: allows script engines to customise request object.
- refactor: don't initialise property list for each request proxy.
- refactor: splits modern and compat Graal implementations.
- test(soap): improves coverage of parser attribute reader.

### Fixed
- fix: support messages having a part attribute without namespace prefix

## [3.39.0] - 2024-05-07
### Added
- feat: allows store items to be read as JSON.

### Changed
- docs: adds CLI instructions for enabling debug mode for Groovy scripts.
- docs: adds Groovy DSL example.
- docs: adds instructions for debugging Groovy scripts.
- docs: tidies Groovy debugging instructions.

## [3.38.3] - 2024-04-24
### Changed
- chore(deps): bump org.apache.maven.plugins:maven-jar-plugin
- test(vertxweb): improves coverage for kebab-case path param matching.
- test: adds test for kebab-case path params.

### Fixed
- fix(vertxweb): supports kebab-case path params.

## [3.38.2] - 2024-04-22
### Fixed
- fix(soap): parse message part elements with anonymous inner types.

## [3.38.1] - 2024-04-21
### Fixed
- fix(soap): applies message part filter from binding operation.

## [3.38.0] - 2024-04-21
### Added
- feat(soap): improves support for element parts in composite messages.
- feat(soap): supports more combinations of binding style and messages.
- feat: support XML attributes in SOAP request body XPath

### Changed
- refactor(soap): matches type message parts on operation name.
- refactor(soap): optimises document type lookup.
- refactor(soap): reuses parser schema type system instead of recompiling all schemas.
- refactor(soap): reuses schema type system and entity resolver for same WSDL.
- refactor(soap): simplifies synthetic schema generation.

### Fixed
- fix(soap): improves example generation logging.
- fix(soap): matches message element types by qualified name.
- fix(soap): sets namespace of RPC root response element from WSDL target namespace.

## [3.37.0] - 2024-04-18
### Added
- feat(soap): SOAP response body uses typed message part names.
- feat(soap): adds support for RPC style bindings.
- feat(soap): adds support for messages with type attribute.
- feat(soap): working support for WSDL 1.1 messages specifying type not element.

### Changed
- chore(deps): bump org.apache.maven.plugins:maven-jar-plugin
- docs: adds single-file WSDL 1 and SOAP 1.1 example.

### Fixed
- fix: restores previous config file location logging behaviour.

## [3.36.3] - 2024-04-15
### Changed
- docs: documents EntityResolver input stream lifecycle expectations.
- refactor(soap): reuses EntityResolver for the same WSDL file.
- refactor: uses instance-scoped EntityResolver for WSDL parsing and SOAP example generation.
- test: removes unneeded fork configuration.
- test: updates test file comment.

### Fixed
- fix: in SOAP plugin, support WSDL with XML schema include

## [3.36.2] - 2024-04-13
### Changed
- ci: includes imposter-all distro in integration tests.
- ci: integration tests verify scripted examples.
- docs: adds JavaScript example for HBase plugin.
- refactor: moves HBase to external plugin.
- test: improves coverage of form params matchers.
- test: improves coverage of path params matchers.
- test: improves coverage of query params matchers.
- test: improves coverage of request header matchers.

## [3.36.1] - 2024-04-12
### Changed
- build: includes js-graal plugin in all distro.

## [3.36.0] - 2024-04-02
### Added
- feat: adds advanced path params, query params and form params matchers.
- feat: adds advanced request header matchers.
- feat: resource matchers and security conditions support all match operators.

### Changed
- chore(deps): bump actions/setup-python from 5.0.0 to 5.1.0
- chore(deps): bump com.fasterxml.jackson.core:jackson-databind
- chore(deps): bump org.apache.maven.plugins:maven-compiler-plugin
- docs: aligns steps documentation with implementation.
- docs: clarifies behaviour of 'exists' operators.
- docs: clarifies steps introduction.
- docs: describes all supported operators for security conditions.
- docs: describes long form and operators for request matching.
- docs: documents Steps API.
- docs: improves step examples.
- refactor: generalises request match operators.
- refactor: moves conditional name-value pair parsing to companion.
- refactor: moves request pair matching to use common matcher logic.

### Fixed
- fix: upgrade com.fasterxml.jackson.core:jackson-databind from 2.16.1 to 2.16.2

## [3.35.2] - 2024-03-27
### Changed
- chore(deps): bumps base image to 11.0.22_7-jre-jammy.

## [3.35.1] - 2024-03-09
### Changed
- chore(deps): bumps CLI to 0.38.0.
- ci: quieten Gradle output.
- docs: adds GraalVM examples and instructions.
- docs: adds percentage based error response example.
- docs: improves AWS lambda bundle instructions.
- docs: improves CLI installation steps.
- docs: improves latency simulation example.
- docs: improves plugin install instructions.
- docs: lists version requirements in es6 documentation.
- docs: removes unneeded boilerplate from examples.
- docs: updates plugins.
- refactor(openapi): switches fallback response to method ref.
- refactor: makes behaviour handlers return a future.
- refactor: makes route handlers asynchronous.
- refactor: quietens disabled metrics logging.
- refactor: removes deprecated passthroughRoute function.
- refactor: renames ResourceService to HandlerService.
- refactor: simplifies handler wrapping.
- refactor: switches empty fallback response to method ref.
- refactor: unifies completion logging and setting exchange phase.

### Fixed
- fix(awslambda): includes SOAP plugin in builder plugin list.
- fix(awslambda): sets status code to 500 if unset when an exceptional failure occurs.
- fix: don't autocomplete future blocks for long-running tasks.
- fix: don't autocomplete future when forwarding upstream.
- fix: don't autocomplete futures for GraphQL queries.
- fix: don't block the event loop in Vert.x Web handlers.
- fix: moves completion log to after handler completion.

## [3.35.0] - 2024-02-11
### Added
- feat: adds support for archive plugin files.
- feat: upgrades Graal to 22.3.5.

### Changed
- build: switches graal plugin to archive format.
- docs: fixes example methods.

## [3.34.0] - 2024-02-08
### Added
- feat: adds random.any() expression function.

### Changed
- chore(deps): bump actions/github-script from 6 to 7
- chore(deps): bump actions/setup-java from 3 to 4
- chore(deps): bump actions/setup-python from 4.7.1 to 5.0.0
- chore(deps): bump org.apache.maven.plugins:maven-compiler-plugin
- chore(deps): bump org.apache.maven.plugins:maven-project-info-reports-plugin
- chore(deps): bump org.apache.maven.plugins:maven-surefire-plugin

### Fixed
- fix: upgrade com.fasterxml.jackson.core:jackson-databind from 2.16.0 to 2.16.1

## [3.33.4] - 2023-12-30
### Changed
- ci: adds js-graal plugin to release binaries.
- docs: adds links to bundled config steps from config description.
- docs: adds usage example for IMPOSTER_OPENAPI_SPEC_PATH_PREFIX.
- docs: clarifies included plugins in imposter-core Docker image.
- docs: improves fake data section title.
- docs: improves nav.
- docs: links to recursive discovery from env vars table.
- docs: simplifies deployment patterns.
- docs: splits out and expands bundle documentation.
- test: adds local docs test script.

## [3.33.3] - 2023-11-17
### Changed
- build: fix shadow JAR publication for modules with Maven publications.
- ci: switch to prebuilt since binary.
- refactor: replaces openapi and rest distributions with core distribution.

## [3.33.2] - 2023-11-17
### Changed
- build: fix shadow JAR publication for mock-hbase module.

## [3.33.1] - 2023-11-17
### Changed
- build: fix shadow JAR publication for fake-data module.

## [3.33.0] - 2023-11-17
### Added
- feat: adds system expression evaluator to config evaluator list.

### Changed
- docs: describes system placeholders.
- docs: improves bundled Docker instructions.

## [3.32.2] - 2023-11-17
### Changed
- chore(deps): bump com.fasterxml.jackson.core:jackson-databind
- chore(deps): bump org.apache.maven.plugins:maven-surefire-plugin

## [3.32.1] - 2023-11-17
### Changed
- ci: splits out static site build step.
- docs: adds examples link.
- docs: adds mermaid plugin to static site generator.
- docs: improves description of Docker bundle steps.
- docs: improves description of deployment patterns and bundle steps.
- docs: switches config store C4 node to DB.
- test: adds dockerfile to preview docs.

### Fixed
- fix: use relative link to configuration documentation in CORS section.

## [3.32.0] - 2023-11-12
### Added
- feat(openapi): allows the specification path prefix to be set.

### Changed
- chore(deps): bump com.amazonaws:aws-java-sdk-bom
- chore(deps): bump com.amazonaws:aws-lambda-java-events
- chore(deps): bump org.apache.maven.plugins:maven-clean-plugin
- chore(deps): bumps shadow plugin to 7.1.2.
- ci: makes test check name unique.
- docs(openapi): improves instructions for OpenAPI responses.
- docs: adds Docker healthcheck example.
- refactor(openapi): moves plugin-specific env var config to Settings object.
- refactor: uses OpenApiPluginImpl in embedded builder instead of hard-coded paths.
- test: bumps Jakarta dep and adds jaxrs runtime dep.
- test: uses RestAssured get method instead of Kotlin shorthand.

### Other
- spike: adds Java 17 LTS CI build.

## [3.31.6] - 2023-10-26
### Changed
- chore(deps): bump com.amazonaws:aws-lambda-java-core from 1.2.2 to 1.2.3
- chore(deps): bump io.rest-assured:rest-assured from 5.3.0 to 5.3.2
- chore(deps): bumps CLI to v0.34.1.

## [3.31.5] - 2023-10-26
### Fixed
- fix: updates CLI release URL.

## [3.31.4] - 2023-10-26
### Changed
- chore(deps): bumps CLI to v0.33.0.

## [3.31.3] - 2023-10-19
### Changed
- chore(deps): bump actions/setup-python from 4.7.0 to 4.7.1
- chore(deps): bump com.fasterxml.jackson.core:jackson-databind
- chore(deps): bump io.github.classgraph:classgraph
- chore(deps): bump version_vertx from 4.4.4 to 4.4.5
- docs: improves description of default and example values for IMPOSTER_CONFIG_DIR.

### Fixed
- fix: examples/junit-sample/pom.xml to reduce vulnerabilities

## [3.31.2] - 2023-10-02
### Changed
- chore(deps): bump actions/checkout from 3 to 4
- chore(deps): bump docker/setup-qemu-action from 2 to 3
- chore(deps): bumps coroutines to 1.7.3.
- chore(deps): bumps kotlin to 1.9.10.
- docs: fixes configDir flag name for JAR mode.
- refactor: switches response file serving to use Vert.x filesystem.
- test(lambda): improves coverage for openapi spec and examples.

### Fixed
- fix(lambda): resolves static index pages.
- fix: uses critical section for file cache I/O.

## [3.31.1] - 2023-09-26
### Changed
- docs: updates roadmap.

### Fixed
- fix: path parameter extraction shouldn't throw NPE for regex routes.

## [3.31.0] - 2023-09-23
### Added
- feat(lambda): falls back to default Lambda bundle config dir if no config dir set.
- feat: supports IMPOSTER_CONFIG_DIR environment variable in all distributions.

### Changed
- docs: improves configuration location instructions.
- refactor: deprecates IMPOSTER_S3_CONFIG_URL env var in favour of IMPOSTER_CONFIG_DIR.

## [3.30.0] - 2023-09-23
### Added
- feat: precompiles inline script steps.
- feat: shortens script step file property name.

### Changed
- docs: adds example Dockerfile and Compose config.
- docs: adds instructions for custom Docker images.
- docs: better signposts healthcheck endpoint.
- refactor: caches inline scripts.
- refactor: caches processing steps for all resources.
- refactor: moves resource config into step context.
- refactor: renames exchange finalisation logic.
- refactor: replaces custom step deserialisation with Jackson.
- refactor: uses dedicated remote expression evaluator.
- test: improves coverage for inline script steps.

## [3.29.0] - 2023-08-21
### Added
- feat: adds support for inline script steps.

### Changed
- chore(deps): bump version_groovy from 4.0.12 to 4.0.13
- chore(deps): bump version_junit_jupiter from 5.9.3 to 5.10.0
- docs: improves setup instructions for fake-data plugin.
- refactor: adds plumbing for inline script steps.
- refactor: renames nashorn-standalone module to nashorn.

### Fixed
- fix: adds option to improve robustness when encountering configuration errors.

## [3.28.3] - 2023-08-07
### Changed
- docs: describes interaction between OpenAPI server path and basePath.

### Fixed
- fix: improves logging of plugin config.

## [3.28.2] - 2023-08-07
### Changed
- refactor: adds description to match result.

### Fixed
- fix: improves logging of request body config.

## [3.28.1] - 2023-08-07
### Changed
- chore(deps): bump version_vertx from 4.4.1 to 4.4.4

### Fixed
- fix: body matching score should include weight of allOf/anyOf clauses.
- fix: don't propagate null through request body match branches
- fix: improves resource matching logging.
- fix: operator matching should use equality check not identity.

## [3.28.0] - 2023-08-04
### Added
- feat: adds system template placeholders.

### Changed
- build: removes separate Nashorn plugin.
- chore(deps): bumps swagger-models to 2.2.14, swagger-parser to 2.1.16 and swagger-request-validator to 2.35.2.

### Fixed
- fix(openapi): explicitly set parseOptions.resolveFully

## [3.27.0] - 2023-08-02
### Added
- feat: adds request.path and request.uri template placeholders.

### Changed
- docs: organises table of contents.

## [3.26.0] - 2023-08-02
### Added
- feat: adds remote processing step.
- feat: adds response.statusCode expression.
- feat: adds script processing step.
- feat: adds syntactic sugar for scripted failure simulation.
- feat: allows remote URL to be templated.
- feat: passes through form params in remote step.
- feat: passes through remote query params.
- feat: passes through remote request headers and templates body content.
- feat: supports relative URLs in remote steps.

### Changed
- chore(deps): bump actions/setup-python from 4.6.1 to 4.7.0
- docs: describes failure simulation effects and improves examples.
- refactor: adds syntactic sugar for remote context steps.
- refactor: narrows interface for step processor.
- refactor: removes unneeded step allocations.
- refactor: supports subset of capture config in remote steps.
- refactor: tidies remote request builder.

## [3.25.4] - 2023-08-02
### Fixed
- fix: improves reliability of static content route detection.

## [3.25.3] - 2023-08-01
### Fixed
- fix(openapi): tolerate absent responses block in operation.

## [3.25.2] - 2023-08-01
### Fixed
- fix: don't treat wildcard routes as unique.

## [3.25.1] - 2023-07-08
### Changed
- chore: bumps Kotlin to 1.8.22.
- test: work-around for Mockito generic bounds issue with Kotlin 1.9.

### Fixed
- fix: trims entries when splitting comma-delimited config.

## [3.25.0] - 2023-07-05
### Added
- feat: adds support for AWS temporary credentials via STS.

## [3.24.1] - 2023-07-01
### Changed
- docs: fixes transposed rows in plugins documentation.

### Fixed
- fix: [#430] swagger-parser update to v2.1.15 in line with jackson v2.15.2 update

## [3.24.0] - 2023-06-25
### Added
- feat: security request header regex operator
- feat: supports Matches and NotMatches operators in security conditions.

### Changed
- chore(deps): bump version_jackson from 2.14.2 to 2.15.2

## [3.23.2] - 2023-06-17
### Fixed
- fix: don't set basepath for root path if empty or no response config.

## [3.23.1] - 2023-06-17
### Changed
- chore(deps): bump version_groovy from 4.0.8 to 4.0.12
- docs: describes body match operators.

### Fixed
- fix: workaround for 'getCallerClass' warning due to log4j repackaging.

## [3.23.0] - 2023-06-12
### Added
- feat: adds basePath property to OpenAPI, REST and SOAP configuration.
- feat: reads config file from disk once.

### Changed
- chore(deps): bump actions/setup-python from 4.6.0 to 4.6.1
- chore(deps): bump io.github.classgraph:classgraph
- chore(deps): bump maven-project-info-reports-plugin
- chore(deps): bump version_junit_jupiter from 5.9.2 to 5.9.3
- docs: describes configuration discovery and basePath.

### Fixed
- fix: do not require a root response configuration when applying base path.
- fix: don't initialise Redis client until first use.
- fix: improves error message when plugin is missing from configuration.

## [3.22.1] - 2023-06-11
### Fixed
- fix: Revert "fix: sets Vert.x base cache directory on Windows."

## [3.22.0] - 2023-05-22
### Added
- feat(soap): improves error detection for missing WSDL elements/attributes.

### Changed
- test(lambda): adds test for duplicate SOAP endpoints.
- test(lambda): improves coverage for using SOAP plugin with Lambda adapter.
- test(lambda): removes dependency on mock S3 for most tests.

### Fixed
- fix(lambda): removes lazy delegates from single use properties.
- fix: don't initialise DynamoDB client until first use.
- fix: duplicate HTTP routes should replace existing ones.

## [3.21.1] - 2023-05-22
### Changed
- build: adds Docker ecosystem to Dependabot config.

### Fixed
- fix: sets Vert.x base cache directory on Windows.

## [3.21.0] - 2023-05-18
### Added
- feat: bundles groovy-xml dependency.

### Changed
- docs: adds form params example.
- docs: describes how to retrieve the configuration directory in a script.
- test: improves coverage of HTTP request matchers.

## [3.20.4] - 2023-05-13
### Changed
- build: bumps since to v0.13.0.
- build: ignores junit-sample changes from changelog.
- build: moves since hooks to config file.

## [3.20.3] - 2023-05-13
### Changed
- docs: adds example showing how to get the directory containing the config in a script.
- docs: describes how to add a JAR to Groovy classpath.
- docs: describes how to pin version.
- docs: describes how to use a local JAR with the CLI.
- docs: describes more expression types.
- docs: improves usage instructions.
- docs: moves Groovy classpath guidance to Groovy tips section.
- refactor: renames internal config dir variable.

## [3.20.2] - 2023-05-11
### Changed
- docs: fixes paths to example projects.
- docs: improves fake data examples.
- docs: improves fake data plugin description.

### Fixed
- fix: removes ambiguous 'name' fake property replacement.

## [3.20.1] - 2023-05-09
### Changed
- docs: describes fake data configuration.

### Fixed
- fix: adds fake-data plugin to 'all' distro.

## [3.20.0] - 2023-05-09
### Added
- feat: adds expression support for fake data generator.
- feat: automatically registers plugins that are lifecycle listeners.

## [3.19.0] - 2023-05-08
### Added
- feat(openapi): support schema 'types' as well as 'type' for OAS 3.1 compatibility.
- feat: adds Datafaker plugin.

### Changed
- ci: adds workflow to test example project.
- docs: fixes JVM embedded dependencies.
- refactor(openapi): more idiomatic example building.

## [3.18.0] - 2023-05-04
### Added
- feat: inherits Groovy scripting classloader from plugin classloader.

### Changed
- build: adds since release config.

## [3.17.3] - 2023-04-30
### Changed
- chore(deps): bump actions/setup-python from 4.5.0 to 4.6.0 (#376)
- chore(deps): bump maven-project-info-reports-plugin (#374)
- ci: bumps version of since.
- docs(examples): adds single WSDL file example.
- docs: improves instructions for REST plugin.
- docs: splits Lambda deployment approaches into separate files.

## [3.17.2] - 2023-04-23
### Added
- docs(lambda): documents support for ARM64 architecture.
- docs(lambda): adds troubleshooting step for anonymous URL access.

### Fixed
- fix: lowercases Lambda request header keys.

## [3.17.1] - 2023-04-22
### Added
- feat: adds test for OpenAPI 3.1 specification containing JWT contentMediaType.

### Fixed
- fix(openapi): improves null safety of schema example generator via idiomatic Kotlin.
- docs(lambda): changes references from JAR file to ZIP.

### Changed
- refactor(openapi): inlines trivial default value providers.

## [3.17.0] - 2023-04-19
### Added
- feat: allows default YAML code point limit to be overridden using `IMPOSTER_YAML_CODE_POINT_LIMIT` environment variable.

### Fixed
- fix: returns engine version from Lambda status endpoint.

## [3.16.3] - 2023-04-17
### Fixed
- fix: prevents resource match if all results have no config.
- feat: adds meaningful toString() functions to resources.
- fix: sets resource match result weight to zero if not matched. 

## [3.16.2] - 2023-04-16
### Fixed
- fix(soap): includes parent namespace declarations when parsing inline schemas. (#357)

## [3.16.1] - 2023-04-14
### Fixed
- fix: improves handling of requests for nonexistent files.

## [3.16.0] - 2023-04-14
### Added
- feat: Adds CORS support. (#366)
- feat: adds static directory response type.

### Fixed
- fix(lambda): don't send duplicate 'not found' response if no route matched.
- fix: don't call response.end() twice if sending string body.

### Changed
- refactor: uses HTTP route to determine trailing wildcard matches.

## [3.15.0] - 2023-04-09
### Added
- feat: allows XPath XML namespaces to be configured at the system level.
- feat: allows matching against one or more body matchers.
- feat: adds eval resource matching to SOAP plugin.
- feat: filter resource matches using score.

### Fixed
- fix: improves trapping of configuration file parsing exceptions.
- fix: corrects plugin path logging on unsupported plugin.

### Changed
- refactor: separates script DSL from response behaviour.
- chore(deps): bump version_jackson from 2.14.1 to 2.14.2 (#346)
- chore(deps): bump version_vertx from 4.3.7 to 4.4.1 (#347)
- ci: sets release to draft until all assets uploaded.
- ci: switches to GITHUB_OUTPUT to set output parameter.

## [3.14.0] - 2023-03-30
### Added
- feat: adds inline scripted resource matcher.

### Fixed
- fix(lambda): URL decodes form parameters.

### Changed
- chore: bumps Kotlin to 1.8.10.
- chore: bumps Gradle to 7.6.1.
- chore(deps): bump com.amazonaws:aws-java-sdk-core (#344)

## [3.13.0] - 2023-03-23
### Added
- feat: supports placeholders in response headers.
- feat: supports reading request form attributes.

### Changed
- chore(deps): bump com.apurebase:kgraphql from 0.17.14 to 0.19.0 (#320)

## [3.12.2] - 2023-03-20
### Fixed
- fix: appends request path to upstream path.

## [3.12.1] - 2023-03-20
### Fixed
- fix: SOAP HTTP binding should use Envelope by default.

### Changed
- chore: bumps CLI to 0.28.0.

## [3.12.0] - 2023-03-16
### Added
- feat: supports trailing wildcards in resource paths.
- feat: allows requests to be proxied to an upstream server.
- feat: improves logging for request body matching.
- docs: adds instructions for Lambda deployment using CLI.

### Fixed
- fix: removes unnecessary non-null assertion.

### Changed
- chore: bumps CLI to 0.27.1.

## [3.11.3] - 2023-03-05
### Changed
- chore(deps): bump actions/checkout from 2 to 3 (#313)
- chore(deps): bump com.amazonaws:aws-java-sdk-core (#317)
- ci: pins changelog parser version.

## [3.11.2] - 2023-03-04
### Changed
- ci: switches to changelog-parser.

## [3.11.1] - 2023-03-01
### Changed
- chore(deps): bump com.amazonaws:aws-java-sdk-core from 1.12.325 to 1.12.417

## [3.11.0] - 2023-03-01
### Added
- feat: adds SOAP plugin to Lambda default plugins.
- test: improves coverage for response service, response file transmission, failure simulation and performance simulation.
- docs: adds CORS example.

### Fixed
- build: fixes target compatibility for nashorn plugin.

### Changed
- build(deps): bump com.atlassian.oai:swagger-request-validator-core from 2.30.0 to 2.33.1 (#299)
- build(deps): bump swagger-parser to 2.1.9 (#299)

## [3.10.0] - 2023-02-18
### Added
- feat: allows templates to access to stores via `stores.` prefix.
- docs: adds end to end store/template example.
- docs: describes random placeholders.

### Changed
- refactor: removes commons-io dependency.
- refactor: moves template logic to engine.
- test: defaults launch mode to java in dev convenience script.

## [3.9.0] - 2023-02-08
### Added
- feat: includes SOAP plugin in core distribution and AWS Lambda distribution.

## [3.8.2] - 2023-02-07
### Added
- feat(wiremock): adds support for fixed and uniform delays.

### Changed
- build(deps): bump io.rest-assured:rest-assured from 5.2.0 to 5.3.0 (#298)
- build(deps): bump jaxen from 1.2.0 to 2.0.0 (#271)
- build(deps): bump version_groovy from 4.0.7 to 4.0.8 (#297)

## [3.8.1] - 2023-02-07
### Added
- ci: generates changelog on release.

## [3.8.0] - 2023-02-02
### Added
- feat: adds support for WireMock format mappings and WireMock response templates.

## [3.7.2] - 2023-02-02
### Added
- feat: adds null-safe function for setting response headers.

### Changed
- build(deps): bump version_vertx from 4.3.6 to 4.3.7 (#290)

## [3.7.1] - 2023-01-22
### Changed
- refactor: allows REST plugin to be extended.
- build(deps): bumps Kotlin to 1.7.22.
- build(deps): bump force-rest-api from 0.0.44 to 0.0.45 (#283)

## [3.7.0] - 2023-01-10
### Added
- feat: adds failure simulation.
- feat: supports multiple request body matchers.
- feat: allows matching body based on existence of node at JsonPath/XPath expression.
- feat: adds string contains check and negative check for request body matching.
- feat: adds regex body matcher.
- feat: adds negative contains body matcher.
- feat: adds syntax to normalise XPath expressions.
- feat: adds random value expression evaluator.

### Fixed
- fix: improves error trapping in expression evaluator.

### Changed
- refactor: moves performance delay to idiomatic Kotlin random generator.
- chore: bumps hamcrest to 2.2.

## [3.6.0] - 2023-01-07
### Added
- feat: adds XPath query support to expressions.
- feat(lambda): loads additional plugins and modules via environment variables.

### Changed
- test: improves coverage for XPath utils.
- feat: allows multiple config resolvers to handle a single path.

## [3.5.2] - 2023-01-03
### Added
- docs: adds example for SOAP request body matching with XPath.
- docs: improves template store placeholder example.

### Fixed
- fix: improves error message when plugin cannot be found.
- fix(soap): quietens logging for XSD element resolution.
- fix: removes environment evaluator from default list.

### Changed
- build(deps): bump version_groovy from 4.0.6 to 4.0.7 (#276)
- chore: bumps CLI to 0.24.1.

## [3.5.1] - 2023-01-01
### Fixed
- fix: SOAP request body matching should work for XPath expressions.

### Added
- docs: adds description for template expressions.

## [3.5.0] - 2022-12-27
### Added
- feat: allows expressions to be used in response templates.
- feat: adds support for expression fallback values.

### Changed
- feat: removes commons-text dependency in favour of expression util.
- feat: only rebuffers template response if changed.
- refactor: simplifies request scoped store handling in templates.
- build(deps): bump aws-java-sdk-bom from 1.12.357 to 1.12.372 (#274)
- build(deps): bump testcontainers from 1.17.3 to 1.17.6 (#266)

## [3.4.0] - 2022-12-23
### Added
- feat: adds support for SOAP HTTP binding transport type.
- feat: adds support for using SOAP 1.2 binding with WSDL 1.
- test: splits parser tests to support more combinations of WSDL1 and SOAP 1.1/1.2.

### Changed
- build(deps): bump version_jackson from 2.13.4 to 2.14.1 (#261)
- build(deps): bump version_vertx from 4.3.5 to 4.3.6 (#267)
- build(deps): bump xmlbeans from 5.1.0 to 5.1.1 (#260)

## [3.3.0] - 2022-12-08
### Added
- feat(soap): adds support for multiple schemas within a WSDL file.

### Fixed
- fix(soap): use message attribute to locate message element in WSDL1 parser.
- fix(soap): operation style should fall back to binding style in WSDL1 parser.

### Changed
- feat: changed OpenAPI Docker image working directory to `/opt/imposter/config` so `$ref` paths load correctly (thanks, Raymond Chin)

## [3.2.3] - 2022-12-03
### Changed
- build(deps): bump actions/setup-python from 4.2.0 to 4.3.0 (#228)
- build(deps): bump aws-java-sdk-core from 1.12.205 to 1.12.325 (#218,#239)
- build(deps): bump byte-buddy-agent from 1.12.10 to 1.12.17 (#217)
- build(deps): bump byte-buddy-dep from 1.12.7 to 1.12.19
- build(deps): bump classgraph from 4.8.54 to 4.8.149 (#229)
- build(deps): bump localstack from 1.17.3 to 1.17.6
- build(deps): bump micrometer-registry-prometheus from 1.9.2 to 1.9.4 (#219)
- build(deps): bump mockito-core from 3.10.0 to 4.9.0
- build(deps): bump redisson from 3.17.0 to 3.17.7 (#242)
- build(deps): bump rest-assured from 5.1.1 to 5.2.0 (#227)
- build(deps): bump s3mock-testcontainers from 2.4.10 to 2.8.0 (#223)
- build(deps): bump swagger-request-validator-core from 2.27.2 to 2.30.0 (#200)
- build(deps): bump version_graal from 22.2.0 to 22.2.0.1 (#232)
- build(deps): bump version_groovy from 4.0.4 to 4.0.6
- build(deps): bump version_jackson from 2.13.3 to 2.13.4 (#226)
- build(deps): bump version_vertx from 4.3.2 to 4.3.4 (#225)
- build(deps): jackson-databind to 2.13.4.2.
- refactor: fixes some deprecated method calls.

### Added
- test: adds recursive config scan option to dev convenience script.

## [3.2.2] - 2022-10-21
### Changed
- build(deps): bump version_commons_text from 1.9 to 1.10.0.

## [3.2.1] - 2022-09-13
### Changed
- refactor: renames `withData` script function to `withContent`. Retains deprecated function name for backwards compatibility.

## [3.2.0] - 2022-09-13
### Fixed
- fix(rest): skips adding route for root resource if response configuration is blank.

### Changed
- refactor: renames `staticData` to `content` in response config. Also adds an alias to ensure backwards compatibility.
- refactor: renames `staticFile` to `file` in response config. Also adds an alias to ensure backwards compatibility.

## [3.1.0] - 2022-09-08
### Added
- feat: enables script file configuration inheritance.
- feat: enables request and response body to be included in structured log entries.
- docs: describes proxy mode and updates getting started.
- docs: describes scaffolding and updates getting started.

### Changed
- chore: bumps CLI to 0.21.0.
- build(deps): bump actions/setup-python from 4.1.0 to 4.2.0 (#198)
- build(deps): bump maven-project-info-reports-plugin (#203)

## [3.0.4] - 2022-08-01
### Changed
- build(deps): bumps CLI to 0.16.1.

## [3.0.3] - 2022-08-01
### Added
- docs: improves build, capture and docker pages.

### Changed
- build(deps): bumps CLI to 0.16.0.
- build(deps): bumps JRE to 11.0.16+8.
- build(deps): bumps Kotlin to 1.7.10.
- build(deps): bump swagger-parser from 2.0.32 to 2.1.1 (#183)
- build(deps): bump aws-java-sdk-bom from 1.12.205 to 1.12.272 (#187)
- build(deps): bump version_jackson from 2.13.2 to 2.13.3 (#188)
- build(deps): bump version_graal from 22.1.0.1 to 22.2.0 (#179)
- build(deps): bump testcontainers from 1.15.3 to 1.17.3 (#180)
- build(deps): bump localstack from 1.17.2 to 1.17.3 (#182)
- build(deps): bump version_log4j from 2.17.2 to 2.18.0 (#178)
- build(deps): bump xmlbeans from 5.0.3 to 5.1.0 (#181)
- build(deps): bump micrometer-registry-prometheus from 1.6.7 to 1.9.2 (#169)
- build(deps): bump nashorn-core from 15.3 to 15.4 (#175)
- build(deps): bump version_coroutines from 1.6.2 to 1.6.4 (#176)
- build: removes unused jcenter repo.
- build: moves restassured dependency and associated constraints to module.
- chore: bumps groovy to 4.0.4.
- chore: bumps restassured to 5.1.1.

## [3.0.2] - 2022-07-25
### Changed
- chore: bumps Vert.x to 4.3.2.
- build: bumps Maven example project dependencies.

## [3.0.1] - 2022-06-27
### Added
- docs: improves getting started.

### Changed
- build: removes package lock files from example projects.
- build(deps): bump localstack from 1.16.3 to 1.17.2 (#153)
- build(deps): bump byte-buddy-agent from 1.12.7 to 1.12.10 #152
- build(deps): bump version_graal from 22.0.0.2 to 22.1.0.1 #150

## [3.0.0] - 2022-06-11
### Added
- feat: adds handler for API Gateway V2 and Function URL events.
- docs: adds serverless example and instructions for using Lambda Function URL.
- docs(docker): improves Docker documentation example.
- feat(openapi): allows spec and UI to be disabled.
- feat(openapi,rest): adds friendly 'not found' response for clients accepting HTML.
- feat: allows META-INF scan to be enabled in Lambda.
- feat: improves request logging.
- test: sets region in DynamoDB test.
- test: bumps S3Mock version to gain aarch64 support.

### Fixed
- fix(awslambda): handle base64 encoded request body.
- fix(openapi): removes server base path from serving prefix.
- fix(openapi): allows adding server entry with path only.
- fix(openapi): separates serving prefix from specification path prefix.
- fix: resource matching should require all config entries to be present in request.
- fix: uses plugin classloader for meta-detector plugin classpath scan.

### Changed
- BREAKING CHANGE: drops support for Java 8. Imposter no longer supports Java 8, due to the complexity of maintaining multiple codepaths depending on target JVMs. If Java 8 support is required, Imposter 2.x is still available, but may no longer be maintained.
- BREAKING CHANGE: removes deprecated script context properties. As signalled in version 2.x, deprecated script context properties have been removed. Removal of legacy `context.params` map - use `context.request.queryParams` instead. Removal of legacy `context.request.params` map - use `context.request.queryParams` instead. Removal of legacy `context.uri` map - use `context.request.uri` instead.
- BREAKING CHANGE: makes request header keys lowercase by default. Effectively sets IMPOSTER_NORMALISE_HEADER_KEYS=true.
- BREAKING CHANGE: uses Query instead of Scan operation in DynamoDB store. Removes IMPOSTER_DYNAMODB_SCAN_TO_LIST_ALL and always uses Query operation to list items.
- BREAKING CHANGE: switches AWS Lambda distro to ZIP file.
- feat: removes UseCGroupMemoryLimitForHeap from container environment. Container aware memory allocation is enabled by default on Java 11.
- refactor: moves HTTP core into separate module.
- refactor: removes unneeded classgraph dep from core config.
- refactor(awslambda): moves inmem dependency up into Lambda adapter.
- refactor(awslambda): moves some logging dependencies to runtime scope.
- refactor: moves dynamic plugin discovery to separate module.
- refactor: improves S3 config resolver module name.
- refactor: moves config resolver properties to separate files.
- refactor: removes use of internal deprecated request accessors.
- build(deps): bump version_graal from 21.2.0 to 22.0.0.2 (#86)
- chore: bumps Groovy to 3.0.10.
- ci: always build dev docker image and run integration tests.
- ci: always publish test results, even on prior step failure.

## [2.14.3] - 2022-05-23
### Fixed
- fix(awslambda): handle base64 encoded request body.

## [2.14.2] - 2022-05-22
### Fixed
- fix(openapi): removes server base path from serving prefix.

## [2.14.1] - 2022-05-22
### Fixed
- fix(openapi): allows adding server entry with path only.

## [2.14.0] - 2022-05-16
### Added
- feat: allows META-INF scan to be enabled in Lambda.

## [2.13.5] - 2022-05-16
### Fixed
- fix(openapi): separates serving prefix from specification path prefix.

## [2.13.4] - 2022-05-15
### Changed
- ci: reintroduces publishing for nashorn-standalone plugin.

## [2.13.3] - 2022-05-09
### Fixed
- fix: resource matching should require all config entries to be present in request.

## [2.13.2] - 2022-05-06
### Fixed
- fix: uses plugin classloader for meta-detector plugin classpath scan.

## [2.13.1] - 2022-04-28
### Added
- feat: allows plugin arguments to be set using environment variables.

### Changed
- feat: adds engine lifecycle hook for startup errors.
- feat: allows Lambda plugin discovery strategy to be set.
- refactor: moves runtime context hook to script lifecycle.
- build(deps): bump aws-java-sdk-core from 1.12.201 to 1.12.205 (#124)
- build(deps): bump aws-java-sdk-bom from 1.12.191 to 1.12.205 (#126)

### Fixed
- fix: sets version in Docker image for CLI consumption.

## [2.13.0] - 2022-04-24
### Added
- ci: publishes nashorn standalone maven artifacts.
- docs: adds test logging configuration to JUnit sample.
- docs: adds example environment variable config for JUnit sample on Java 8.

### Changed
- build: upgrades to Kotlin 1.6.
- build: upgrades to Vert.x 4. (#89)
- build(deps): bump aws-lambda-java-events from 3.9.0 to 3.11.0 (#96)
- build(deps): bump aws-lambda-java-log4j2 from 1.5.0 to 1.5.1 (#102) (#112)
- build(deps): bump aws-java-sdk-core from 1.11.656 to 1.12.201 (#119)
- build(deps): bump commons-io from 2.9.0 to 2.11.0 (#111)
- build(deps): bump guice from 4.2.3 to 5.1.0 (#99)
- build(deps): bump jaxb-api from 2.3.0 to 2.3.1 (#95)
- build(deps): bump redisson from 3.16.0 to 3.17.0 (#108)
- build(deps): bump swagger-parser from 2.0.25 to 2.0.32 (#98)
- build(deps): bump swagger-request-validator-core from 2.27.1 to 2.27.2 (#120)
- build(deps): bump version_coroutines from 1.6.0 to 1.6.1 (#105)
- chore: bumps Graal to 21.3.1.

### Fixed
- fix: prefer routes with exact matches over those with path placeholders.

## [2.12.0] - 2022-04-04
### Added
- build: builds both arm64 and amd64 Docker images.
- fix: strips surrounding quotes from when determining plugin class.

### Changed
- chore: bumps JRE to 8u322.
- chore: bumps log4j2 to 2.17.2.
- chore: bumps vert.x to 3.9.13.
- chore: bumps CLI to 0.12.6.
- chore: bumps jackson-databind to 2.13.2.2.
- chore: bumps JUnit to 2.13.1.

## [2.11.1] - 2022-03-22
### Fixed
- fix: rewrites spec paths with base path in OpenAPI plugin.
- refactor: replaces Vert.x executeBlocking with coroutines at boot. This should reduce the logging noise on slow startup.

### Changed
- chore: bumps CLI to 0.12.3.
- chore: bumps Jackson to 2.13.2.
- docs: switches CLI plugin install guidance to use 'save default' option.
- docs: improves formatting of .imposterignore example.

## [2.11.0] - 2022-03-21
### Added
- feat: improves dynamic Groovy script loading.
- feat: adds graphql query support for stores.
- feat: adds support for '.imposterignore' file when scanning for config files.
- docs: adds future JVM version change to roadmap.
- docs: adds details about using path to set a prefix in the OpenAPI mock.

## [2.10.0] - 2022-03-12
### Added
- feat: allows store queries by key prefix.
- feat: allows use of query operation in place of scan in DynamoDB store driver.
- feat: adds support for JSON parsing in Groovy scripts.

### Changed
- refactor: simplifies plugin discovery and provider logic.

## [2.9.1] - 2022-03-07
### Added
- feat: allows JsonPath queries in expressions.

## [2.9.0] - 2022-02-27
### Added
- feat: allows pathless SOAP operation matches.
- feat: adds support for XPath request body capture.
- feat: adds XPath resource matching.
- feat: adds support for multiple resource configurations with the same path/method in REST plugin.

### Changed
- refactor: moves initialisation logic from verticle to builder and engine.
- refactor: optimises JsonPath and XPath queries.

### Fixed
- fix: improves resource matching with multiple rules for the same path.

## [2.8.3] - 2022-02-25
### Fixed
- fix: caches request and response wrappers in Vert.x adapter.
- fix: moves deferred capture and store cleanup to after response transmission to support delayed responses.

## [2.8.2] - 2022-02-22
### Added
- feat: adds DynamoDB and Redis store drivers to 'all' distribution.

### Fixed
- fix: improves handling of nonexistent response files.
- fix: supports session token in DynamoDB store driver.

## [2.8.1] - 2022-02-07
### Fixed
- fix: uses dedicated XSD parser for WSDL schemas.

## [2.8.0] - 2022-02-07
### Added
- feat: adds SOAP mocks plugin. This is available as a plugin and in the 'all' distro. See the user documentation for plugin reference and examples.
- feat: allows resource matching without specifying an HTTP method.
- feat: allows capture of store items to be deferred to after request processing has completed.
- feat: adds support for capture of response headers and body.
- docs: improves expression description and examples.

### Changed
- feat: response file cache now applies to all files, not just templates.
- docs: moves examples to top level directory.
- chore: bumps sample serverless dependency to 2.72.2.

## [2.7.3] - 2022-01-31
### Added
- feat: allows store name to be set using a capture config.
- feat: allows use of expressions for store name.
- feat: allows constant values for key name.

## [2.7.2] - 2022-01-26
### Changed
- refactor: moves mock implementations to dedicated subdirectory.
- refactor: replaces HBase and SFDC distributions with plugins.
- chore: bumps CLI to 0.10.1.

## [2.7.1] - 2022-01-26
### Added
- feat: adds map data type support to DynamoDB store.

### Changed
- refactor: switches Lambda adapter to static plugin discovery to improve boot time.
- refactor: short-circuits in memory store creation if unmodified.
- refactor: defers in memory store instantiation until first use.

### Fixed
- fix: non-existent stores no longer throw an exception.

## [2.7.0] - 2022-01-20
### Added
- feat: adds expressions for capture keys and values.

### Changed
- refactor: splits embedded distro into submodules.
- refactor: adds Java version guard to standalone script service.

### Fixed
- fix: improves accuracy of Nashorn error line numbers.
- fix: improves error trapping in Vert.x Web error handler.

## [2.6.4] - 2022-01-17
### Added
- feat: allows envfile resolution to be disabled.

## [2.6.3] - 2022-01-17
### Added
- feat: adds envfile resolution relative to configuration files.

## [2.6.2] - 2022-01-17
### Added
- feat: allows OpenAPI path prefix to be configured.
- feat: adds support for envfiles.

## [2.6.1] - 2022-01-17
### Fixed
- fix: stops S3 config downloader from attempting to download directories.
- chore: bumps sample project dependency 'follow-redirects' to 1.14.7.

## [2.6.0] - 2022-01-14
### Added
- feat: supports recursive configuration file discovery.
- feat: adds option to cache remote OpenAPI specifications on local filesystem.

### Fixed
- fix: ensures Redis and DynamoDB stores always indicate they exist.
- ci: removes Graal plugin release asset.

## [2.5.4] - 2022-01-12
### Changed
- chore: bumps CLI to 0.9.4.
- chore: bumps vert.x to 3.9.12.

## [2.5.3] - 2022-01-11
### Fixed
- refactor: uses Nashorn APIs to instantiate script engine to disambiguate implementation.

## [2.5.2] - 2022-01-11
### Added
- feat: adds standalone Nashorn JavaScript implementation.
- feat: allows Graal.js to be used as JavaScript implementation using plugin system.
- feat: automatically detects store driver plugins on the classpath.
- build: adds Java 11 build.

### Changed
- refactor: removes support for legacy plugin package.
- chore: bumps CLI to 0.9.1.

## [2.5.1] - 2022-01-10
### Added
- feat: allows plugin classloader strategy to be configured.

## [2.5.0] - 2022-01-09
### Added
- feat: improves supported data types for DynamoDB store.
- feat: adds support for TTL in DynamoDB store.
- feat: denies recursive templating of untrusted response data by default.
- feat: increases speed of Groovy scripts by two orders of magnitude.
- feat: functionally aligns Graal with Nashorn for JavaScript engine.
- feat: introduces 'core' distribution supporting OpenAPI and REST plugins.

### Changed
- build: replaces server-bundled store implementations with standalone plugins.
- refactor: routes all plugin-like classloading through dedicated ClassLoader.
- chore: bumps CLI to 0.8.2.

## [2.4.14] - 2022-01-08
### Changed
- feat: switches Lambda adapter logging to AWS Lambda appender.
- feat: improves file not found logging.

### Fixed
- fix: skips OpenAPI plugin setup if no compatible configuration files are provided.

## [2.4.13] - 2022-01-05
### Changed
- feat: sets explicit uid/gid for container user.

## [2.4.12] - 2021-12-29
### Changed
- chore: bumps log4j2 to 2.17.1.

## [2.4.11] - 2021-12-28
### Changed
- refactor: removes log4j S3 appender from lambda adapter.

## [2.4.10] - 2021-12-27
### Changed
- chore: bumps Groovy to 2.5.15.
- chore: bumps Guice to 4.2.3.
- refactor: removes unneeded Gson dependency from Lambda adapter.

## [2.4.9] - 2021-12-27
### Changed
- chore: bumps okhttp3 to 4.9.3.

## [2.4.8] - 2021-12-22
### Changed
- build: uses managed dependency versions in lambda adapter.

## [2.4.7] - 2021-12-21
### Changed
- feat: uses application specific user in Docker image.
- feat: hardens container image to remove alpine distro bin.
- chore: bumps CLI to 0.8.0.

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
