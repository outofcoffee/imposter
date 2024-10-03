package io.gatehill.imposter.http.util

import io.gatehill.imposter.http.HttpRoute

object PathNormaliser {
    /**
     * The permitted characters in a path parameter name. Note this is more restrictive than the Vert.x format,
     * as it must also support [HttpRoute] regex named capture group names, which are defined here:
     *
     * https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#groupname
     *
     * Vert.x documentation says:
     * > The placeholders consist of : followed by the parameter name.
     * > Parameter names consist of any alphabetic character, numeric character or underscore.
     *
     * See: https://vertx.io/docs/vertx-web/java/#_capturing_path_parameters
     *
     * This regex pattern does not include the colon prefix or surrounding brackets.
     */
    private val SAFE_PATH_PARAM_NAME = Regex("[a-zA-Z0-9]+")

    /**
     * Normalises path parameters to have safe names.
     *
     * If a path parameter name does not match the safe path format, it is replaced with a UUID,
     * and a mapping is added to the `normalisedParams` map.
     *
     * For example:
     * ```
     * /{pathParam}/notParam
     * ```
     * will be maintained as:
     * ```
     * /{pathParam}/notParam
     * ```
     *
     * A path parameter name that does not match the safe format, such as:
     * ```
     * /{param-with-dashes}
     * ```
     * will be converted to:
     * ```
     * /{123e4567e89b12d3a4564266141740000}
     * ```
     * and the mapping `param-with-dashes -> 123e4567e89b12d3a4564266141740000`
     * will be added to the `normalisedParams` map.
     *
     * @param normalisedParams a map to store the normalised parameter names
     * @param rawPath the path to normalise
     */
    fun normalisePath(normalisedParams: MutableMap<String, String>, rawPath: String?): String? {
        var path = rawPath
        if (!path.isNullOrEmpty()) {
            val matcher = HttpRoute.PATH_PARAM_PLACEHOLDER.matcher(path)
            val sb = StringBuffer()
            while (matcher.find()) {
                val finalParamName = matcher.group(1).let { paramName ->
                    if (paramName.matches(SAFE_PATH_PARAM_NAME)) {
                        return@let paramName
                    } else {
                        val existingMapping = normalisedParams.entries.find { it.value == paramName }
                        if (null != existingMapping) {
                            return@let existingMapping.key
                        }

                        val paramIndex = normalisedParams.size + 1
                        val normalisedName = "param${paramIndex}"
                        normalisedParams[normalisedName] = paramName
                        return@let normalisedName
                    }
                }
                matcher.appendReplacement(sb, "{$finalParamName}")
            }
            path = matcher.appendTail(sb).toString()
        }
        return path
    }

    fun getNormalisedParamName(normalisedParams: Map<String, String>, originalParamName: String): String {
        if (originalParamName.matches(SAFE_PATH_PARAM_NAME)) {
            return originalParamName
        }
        return normalisedParams.entries.find { it.value == originalParamName }?.key ?: originalParamName
    }

    fun denormaliseParams(normalisedParams: Map<String, String>, exchangeParams: Map<String, String>): Map<String, String> {
        // if it's not in the map it doesn't need to be denormalised
        return exchangeParams.mapKeys { normalisedParams[it.key] ?: it.key }
    }
}
