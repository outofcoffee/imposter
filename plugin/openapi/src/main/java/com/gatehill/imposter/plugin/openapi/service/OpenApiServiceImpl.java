package com.gatehill.imposter.plugin.openapi.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.swagger.models.*;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.Parameter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiServiceImpl implements OpenApiService {
    private static final Logger LOGGER = LogManager.getLogger(OpenApiServiceImpl.class);
    private static final String DEFAULT_TITLE = "Imposter Mock APIs";

    @Override
    public Swagger combineSpecifications(List<Swagger> specs, String basePath) {
        return combineSpecifications(specs, basePath, null, null);
    }

    @Override
    public Swagger combineSpecifications(List<Swagger> specs, String basePath, Scheme scheme, String title) {
        requireNonNull(specs, "Input specifications must not be null");
        LOGGER.debug("Generating combined specification from {} inputs", specs.size());

        final Swagger combined = new Swagger();
        combined.basePath(basePath);

        final Info info = new Info();
        combined.setInfo(info);

        final List<Tag> tags = Lists.newArrayList();
        combined.setTags(tags);

        // use set to force uniqueness
        final Set<String> consumes = Sets.newHashSet();
        final Set<String> produces = Sets.newHashSet();

        // note: overridden by scheme parameter
        final List<Scheme> childSchemes = Lists.newArrayList();

        final List<SecurityRequirement> security = Lists.newArrayList();
        combined.setSecurity(security);

        final Map<String, Path> paths = Maps.newHashMap();
        combined.setPaths(paths);

        final Map<String, SecuritySchemeDefinition> securityDefinitions = Maps.newHashMap();
        combined.setSecurityDefinitions(securityDefinitions);

        final Map<String, Model> definitions = Maps.newHashMap();
        combined.setDefinitions(definitions);

        final Map<String, Parameter> parameters = Maps.newHashMap();
        combined.setParameters(parameters);

        final Map<String, Response> responses = Maps.newHashMap();
        combined.setResponses(responses);

        final StringBuilder description = new StringBuilder()
                .append("This specification includes the following APIs:");

        specs.forEach(spec -> {
            ofNullable(spec.getInfo()).ifPresent(specInfo -> description
                    .append("\n* **")
                    .append(specInfo.getTitle())
                    .append("**")
                    .append(ofNullable(specInfo.getDescription()).map(specDesc -> " - " + specDesc).orElse("")));

            tags.addAll(getOrEmpty(spec.getTags()));
            consumes.addAll(getOrEmpty(spec.getConsumes()));
            produces.addAll(getOrEmpty(spec.getProduces()));
            childSchemes.addAll(getOrEmpty(spec.getSchemes()));
            security.addAll(getOrEmpty(spec.getSecurity()));
            securityDefinitions.putAll(getOrEmpty(spec.getSecurityDefinitions()));
            definitions.putAll(getOrEmpty(spec.getDefinitions()));
            parameters.putAll(getOrEmpty(spec.getParameters()));
            responses.putAll(getOrEmpty(spec.getResponses()));

            // prefix paths with base url
            final String childBasePath = ofNullable(spec.getBasePath()).orElse("");
            getOrEmpty(spec.getPaths()).forEach((path, pathDetails) ->
                    paths.put(childBasePath + path, pathDetails));
        });

        combined.setConsumes(Lists.newArrayList(consumes));
        combined.setProduces(Lists.newArrayList(produces));

        if (null != scheme) {
            combined.scheme(scheme);
        } else {
            // use those derived from the child specifications
            combined.setSchemes(childSchemes);
        }

        info.setTitle(ofNullable(title).orElse(DEFAULT_TITLE));
        info.setDescription(description.toString());
        return combined;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getOrEmpty(List<T> list) {
        return ofNullable(list).orElse(Collections.EMPTY_LIST);
    }

    @SuppressWarnings("unchecked")
    private <K, V> Map<K, V> getOrEmpty(Map<K, V> list) {
        return ofNullable(list).orElse(Collections.EMPTY_MAP);
    }
}
