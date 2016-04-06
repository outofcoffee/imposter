package com.gatehill.imposter.plugin;

import com.gatehill.imposter.BaseMockConfig;
import io.vertx.ext.web.Router;

import java.util.List;
import java.util.Map;

/**
 * @author pcornish
 */
public interface Plugin {
    void configureRoutes(Router router);

    void configureMocks(Map<String, BaseMockConfig> mockConfigs);
}
