package com.gatehill.imposter.util;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.config.BaseConfig;
import com.google.common.io.CharStreams;
import io.vertx.core.json.JsonArray;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class FileUtil {
    private FileUtil() {
    }

    public static InputStream loadResponseAsStream(ImposterConfig imposterConfig, BaseConfig mockConfig) throws IOException {
        return Files.newInputStream(Paths.get(imposterConfig.getConfigDir(), mockConfig.getResponseFile()));
    }

    public static JsonArray loadResponseAsJsonArray(ImposterConfig imposterConfig, BaseConfig config) {
        try (InputStream is = loadResponseAsStream(imposterConfig, config)) {
            return new JsonArray(CharStreams.toString(new InputStreamReader(is)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
