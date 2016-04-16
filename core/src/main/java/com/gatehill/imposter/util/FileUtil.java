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
    public static final String CLASSPATH_PREFIX = "classpath:";
    public static final String CONFIG_FILE_SUFFIX = "-config.json";

    private FileUtil() {
    }
}
