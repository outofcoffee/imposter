package com.gatehill.imposter.plugin.openapi.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.models.*;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.Parameter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiServiceImpl implements OpenApiService {
    @Override
    public Swagger combineSpecifications(List<Swagger> specs) {
        final Swagger combined = new Swagger();

        final Info info = new Info();
        combined.setInfo(info);

        final List<String> consumes = Lists.newArrayList();
        combined.setConsumes(consumes);

        final List<String> produces = Lists.newArrayList();
        combined.setProduces(produces);

        final List<Scheme> schemes = Lists.newArrayList();
        combined.setSchemes(schemes);

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
                .append("API specifications:" + "\n<ul>");

        specs.forEach(spec -> {
            ofNullable(spec.getInfo()).ifPresent(specInfo -> description
                    .append("\n<li>")
                    .append(specInfo.getTitle())
                    .append(ofNullable(specInfo.getDescription()).map(specDesc -> "<p>" + specDesc + "</p>").orElse(""))
                    .append("</li>"));

            final String basePath = ofNullable(spec.getBasePath()).orElse("");

            consumes.addAll(getOrEmpty(spec.getConsumes()));
            produces.addAll(getOrEmpty(spec.getProduces()));
            schemes.addAll(getOrEmpty(spec.getSchemes()));
            security.addAll(getOrEmpty(spec.getSecurity()));
            securityDefinitions.putAll(getOrEmpty(spec.getSecurityDefinitions()));
            definitions.putAll(getOrEmpty(spec.getDefinitions()));
            parameters.putAll(getOrEmpty(spec.getParameters()));
            responses.putAll(getOrEmpty(spec.getResponses()));

            // prefix paths with base url
            getOrEmpty(spec.getPaths()).forEach((path, pathDetails) ->
                    paths.put(basePath + path, pathDetails));
        });

        info.setTitle("Combined specification");
        description.append("\n</ul>");
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
