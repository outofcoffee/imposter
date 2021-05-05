package io.gatehill.imposter.plugin.openapi.service;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.SimpleValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig;
import io.gatehill.imposter.plugin.openapi.util.ValidationReportUtil;
import io.gatehill.imposter.util.MapUtil;
import io.swagger.models.Scheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SpecificationServiceImpl implements SpecificationService {
    private static final Logger LOGGER = LogManager.getLogger(SpecificationServiceImpl.class);
    private static final String DEFAULT_TITLE = "Imposter Mock APIs";
    private static final String ARG_BASEPATH = "openapi.basepath";
    private static final String ARG_SCHEME = "openapi.scheme";
    private static final String ARG_TITLE = "openapi.title";

    private final Cache<String, Object> cache = CacheBuilder.newBuilder().build();
    private final SimpleValidationReportFormat reportFormater = SimpleValidationReportFormat.getInstance();

    @Override
    public OpenAPI getCombinedSpec(ImposterConfig imposterConfig, List<OpenAPI> allSpecs) throws ExecutionException {
        return (OpenAPI) cache.get("combinedSpecObject", () -> {
            try {
                final Scheme scheme = Scheme.forValue(imposterConfig.getPluginArgs().get(ARG_SCHEME));
                final String basePath = imposterConfig.getPluginArgs().get(ARG_BASEPATH);
                final String title = imposterConfig.getPluginArgs().get(ARG_TITLE);

                return combineSpecifications(allSpecs, basePath, scheme, title);

            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        });
    }

    @Override
    public String getCombinedSpecSerialised(ImposterConfig imposterConfig, List<OpenAPI> allSpecs) throws ExecutionException {
        return (String) cache.get("combinedSpecSerialised", () -> {
            try {
                return MapUtil.JSON_MAPPER.writeValueAsString(getCombinedSpec(imposterConfig, allSpecs));
            } catch (JsonGenerationException e) {
                throw new ExecutionException(e);
            }
        });
    }

    @Override
    public OpenAPI combineSpecifications(List<OpenAPI> specs, String basePath, Scheme scheme, String title) {
        requireNonNull(specs, "Input specifications must not be null");
        LOGGER.debug("Generating combined specification from {} inputs", specs.size());

        final OpenAPI combined = new OpenAPI();

        final Info info = new Info();
        combined.setInfo(info);

        final List<Server> servers = newArrayList();
        combined.setServers(servers);

        final List<SecurityRequirement> security = newArrayList();
        combined.setSecurity(security);

        final Paths paths = new Paths();
        combined.setPaths(paths);

        final List<Tag> tags = newArrayList();
        combined.setTags(tags);

        final List<ExternalDocumentation> allExternalDocs = newArrayList();
        final List<Components> allComponents = newArrayList();

        final StringBuilder description = new StringBuilder()
                .append("This specification includes the following APIs:");

        specs.forEach(spec -> {
            ofNullable(spec.getInfo()).ifPresent(specInfo -> description
                    .append("\n* **")
                    .append(specInfo.getTitle())
                    .append("**")
                    .append(ofNullable(specInfo.getDescription()).map(specDesc -> " - " + specDesc).orElse("")));

            servers.addAll(getOrEmpty(spec.getServers()));
            security.addAll(getOrEmpty(spec.getSecurity()));
            tags.addAll(getOrEmpty(spec.getTags()));

            if (nonNull(spec.getExternalDocs())) {
                allExternalDocs.add(spec.getExternalDocs());
            }

            if (nonNull(spec.getComponents())) {
                allComponents.add(spec.getComponents());
            }

            paths.putAll(getOrEmpty(spec.getPaths()));
        });

        // info
        info.setTitle(ofNullable(title).orElse(DEFAULT_TITLE));
        info.setDescription(description.toString());

        // external docs
        final ExternalDocumentation externalDocs = new ExternalDocumentation();

        externalDocs.setDescription(allExternalDocs.stream()
                .map(externalDocumentation -> ofNullable(externalDocumentation.getDescription()).orElse(""))
                .collect(Collectors.joining(LINE_SEPARATOR)));

        // NOTE: The OAS spec only permits a single URL, so, to avoid confusion,
        // we don't set it at all.

        combined.setExternalDocs(externalDocs);

        // components
        final Components components = new Components();

        components.setCallbacks(aggregate(allComponents, Components::getCallbacks));
        components.setExamples(aggregate(allComponents, Components::getExamples));
        components.setExtensions(aggregate(allComponents, Components::getExtensions));
        components.setHeaders(aggregate(allComponents, Components::getHeaders));
        components.setLinks(aggregate(allComponents, Components::getLinks));
        components.setParameters(aggregate(allComponents, Components::getParameters));
        components.setRequestBodies(aggregate(allComponents, Components::getRequestBodies));
        components.setResponses(aggregate(allComponents, Components::getResponses));
        components.setSchemas(aggregate(allComponents, Components::getSchemas));
        components.setSecuritySchemes(aggregate(allComponents, Components::getSecuritySchemes));

        combined.setComponents(components);

        setServers(combined, servers, scheme, basePath);

        combined.setPaths(paths);
        combined.setSecurity(security);
        combined.setTags(tags);

        return combined;
    }

    @Override
    public boolean isValidRequest(
            ImposterConfig imposterConfig,
            OpenApiPluginConfig pluginConfig,
            RoutingContext routingContext,
            List<OpenAPI> allSpecs
    ) {
        if (isNull(pluginConfig.getValidation())) {
            LOGGER.trace("Validation is disabled");
            return true;
        }
        if (!Boolean.TRUE.equals(pluginConfig.getValidation().getRequest())) {
            LOGGER.trace("Request validation is disabled");
            return true;
        }
        if (Boolean.TRUE.equals(pluginConfig.getValidation().getResponse())) {
            throw new UnsupportedOperationException("Response validation is not supported");
        }

        final OpenApiInteractionValidator validator;
        try {
            validator = getValidator(imposterConfig, pluginConfig, allSpecs);
        } catch (ExecutionException e) {
            routingContext.fail(new RuntimeException("Error building spec validator", e));
            return false;
        }

        final HttpServerRequest request = routingContext.request();
        final SimpleRequest.Builder requestBuilder = new SimpleRequest.Builder(request.method().toString(), request.path())
                .withBody(routingContext.getBodyAsString());

        request.headers().forEach(h -> requestBuilder.withHeader(h.getKey(), h.getValue()));

        final ValidationReport report = validator.validateRequest(requestBuilder.build());
        if (!report.getMessages().isEmpty()) {
            final String reportMessages = reportFormater.apply(report);
            LOGGER.warn("Validation failed for {} {}: {}", request.method(), request.path(), reportMessages);

            // only respond with 400 if validation failures are at error level
            if (report.hasErrors()) {
                final HttpServerResponse response = routingContext.response();
                response.setStatusCode(400);

                if (pluginConfig.getValidation().getReturnErrorsInResponse()) {
                    ValidationReportUtil.sendValidationReport(routingContext, reportMessages);
                } else {
                    response.end();
                }
                return false;
            }
        } else {
            LOGGER.debug("Validation passed for {} {}", request.method(), request.path());
        }

        return true;
    }

    /**
     * Returns the specification validator from cache, creating it first on cache miss.
     */
    private OpenApiInteractionValidator getValidator(ImposterConfig imposterConfig, OpenApiPluginConfig pluginConfig, List<OpenAPI> allSpecs) throws ExecutionException {
        return (OpenApiInteractionValidator) cache.get("specValidator", () -> {
            final OpenAPI combined = getCombinedSpec(imposterConfig, allSpecs);

            final OpenApiInteractionValidator.Builder builder = OpenApiInteractionValidator.createFor(combined);

            // custom validation levels
            if (nonNull(pluginConfig.getValidation().getLevels())) {
                LOGGER.trace("Using custom validation levels: {}", pluginConfig.getValidation().getLevels());
                final LevelResolver.Builder levelBuilder = LevelResolver.create();
                pluginConfig.getValidation().getLevels().forEach((key, value) -> levelBuilder.withLevel(key, ValidationReport.Level.valueOf(value)));
                builder.withLevelResolver(levelBuilder.build());
            }

            return builder.build();
        });
    }

    private void setServers(OpenAPI combined, List<Server> servers, Scheme scheme, String basePath) {
        if (nonNull(scheme)) {
            servers.forEach(server -> overrideScheme(scheme.toValue(), server));
        }
        if (nonNull(basePath)) {
            if (servers.isEmpty()) {
                final Server server = new Server();
                servers.add(server);
            }
            servers.forEach(server -> prefixPath(basePath, server));
        }
        combined.setServers(servers.stream().distinct().collect(Collectors.toList()));
    }

    /**
     * Override the scheme of the {@link Server} URL.
     *
     * @param requiredScheme the scheme to set
     * @param server         the server to modify
     */
    private void overrideScheme(String requiredScheme, Server server) {
        try {
            final URI original = new URI(server.getUrl());
            if (nonNull(original.getScheme()) && !original.getScheme().equalsIgnoreCase(requiredScheme)) {
                final URI modified = new URI(
                        requiredScheme,
                        original.getUserInfo(),
                        original.getHost(),
                        original.getPort(),
                        original.getPath(),
                        original.getQuery(),
                        original.getFragment()
                );
                server.setUrl(modified.toASCIIString());
            }
        } catch (URISyntaxException ignored) {
        } catch (Exception e) {
            LOGGER.warn("Error overriding scheme to '{}' for server URL: {}", requiredScheme, server.getUrl());
        }
    }

    /**
     * Prefix the path of the {@link Server} URL.
     *
     * @param basePath the path prefix
     * @param server   the server to modify
     */
    private void prefixPath(String basePath, Server server) {
        try {
            final URI original = new URI(server.getUrl());
            final URI modified = new URI(
                    original.getScheme(),
                    original.getUserInfo(),
                    original.getHost(),
                    original.getPort(),
                    basePath + ofNullable(original.getPath()).orElse(""),
                    original.getQuery(),
                    original.getFragment()
            );
            server.setUrl(modified.toASCIIString());
        } catch (URISyntaxException ignored) {
        } catch (Exception e) {
            LOGGER.warn("Error prefixing scheme with '{}' for server URL: {}", basePath, server.getUrl());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getOrEmpty(List<T> list) {
        return ofNullable(list).orElse(Collections.EMPTY_LIST);
    }

    @SuppressWarnings("unchecked")
    private <K, V> Map<K, V> getOrEmpty(Map<K, V> list) {
        return ofNullable(list).orElse(Collections.EMPTY_MAP);
    }

    private <H, T> Map<String, T> aggregate(List<H> allHolders, Function<H, Map<String, T>> mapSupplier) {
        final Map<String, T> all = newHashMap();

        allHolders.stream()
                .map(mapSupplier)
                .filter(Objects::nonNull)
                .forEach(all::putAll);

        return all;
    }
}
