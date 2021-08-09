package io.gatehill.imposter.plugin.openapi.config;

import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiPluginValidationConfig {
    private static final ValidationIssueBehaviour DEFAULT_ISSUE_BEHAVIOUR;

    static {
        DEFAULT_ISSUE_BEHAVIOUR = ValidationIssueBehaviour.from(
                System.getenv("IMPOSTER_OPENAPI_VALIDATION_DEFAULT_BEHAVIOUR"),
                ValidationIssueBehaviour.IGNORE
        );
    }

    /**
     * Could be {@link Boolean} or {@link ValidationIssueBehaviour}
     */
    private String request;

    /**
     * Could be {@link Boolean} or {@link ValidationIssueBehaviour}
     */
    private String response;

    private Boolean returnErrorsInResponse = true;
    private Map<String, String> levels;

    /**
     * Cached default request validation issue behaviour.
     */
    private ValidationIssueBehaviour requestBehaviour;

    /**
     * Cached default response validation issue behaviour.
     */
    private ValidationIssueBehaviour responseBehaviour;

    public ValidationIssueBehaviour getRequest() {
        if (isNull(requestBehaviour)) {
            requestBehaviour = ValidationIssueBehaviour.from(request, DEFAULT_ISSUE_BEHAVIOUR);
        }
        return requestBehaviour;
    }

    public ValidationIssueBehaviour getResponse() {
        if (isNull(responseBehaviour)) {
            responseBehaviour = ValidationIssueBehaviour.from(response, ValidationIssueBehaviour.IGNORE);
        }
        return responseBehaviour;
    }

    public Boolean getReturnErrorsInResponse() {
        return returnErrorsInResponse;
    }

    public Map<String, String> getLevels() {
        return levels;
    }

    /**
     * Supports backwards compatible boolean-style values, mapping
     * to {@link #IGNORE} and {@link #FAIL} respectively.
     */
    public enum ValidationIssueBehaviour {
        IGNORE,
        LOG_ONLY,
        FAIL;

        static ValidationIssueBehaviour from(String behaviour, ValidationIssueBehaviour defaultBehaviour) {
            if (ofNullable(behaviour).map(String::trim).orElse("").isEmpty()) {
                return defaultBehaviour;
            }
            switch (behaviour.trim().toLowerCase()) {
                case "false":
                case "ignore":
                    return IGNORE;

                case "log":
                case "log_only":
                case "log-only":
                    return LOG_ONLY;

                case "true":
                case "fail":
                    return FAIL;

                default:
                    throw new UnsupportedOperationException("Unknown validation issue behaviour: " + behaviour);
            }
        }
    }
}
