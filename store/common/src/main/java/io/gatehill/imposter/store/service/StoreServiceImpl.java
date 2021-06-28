package io.gatehill.imposter.store.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.lifecycle.ImposterLifecycleHooks;
import io.gatehill.imposter.lifecycle.ImposterLifecycleListener;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.script.ExecutionContext;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;
import io.gatehill.imposter.service.ResourceService;
import io.gatehill.imposter.store.model.Store;
import io.gatehill.imposter.store.model.StoreHolder;
import io.gatehill.imposter.store.model.StoreLocator;
import io.gatehill.imposter.util.HttpUtil;
import io.gatehill.imposter.util.MapUtil;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.MIMEHeader;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.ParsableMIMEValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class StoreServiceImpl implements StoreService, ImposterLifecycleListener {
    private static final Logger LOGGER = LogManager.getLogger(StoreServiceImpl.class);
    private static final ParsableMIMEValue JSON_MIME = new ParsableMIMEValue(HttpUtil.CONTENT_TYPE_JSON);

    private final Vertx vertx;
    private final ResourceService resourceService;
    private final StoreLocator storeLocator;
    private final StoreHolder storeHolder;

    @Inject
    public StoreServiceImpl(
            Vertx vertx,
            ImposterLifecycleHooks lifecycleHooks,
            ResourceService resourceService,
            StoreLocator storeLocator
    ) {
        this.vertx = vertx;
        this.resourceService = resourceService;
        this.storeLocator = storeLocator;

        LOGGER.warn("Experimental store support enabled");
        storeHolder = new StoreHolder(storeLocator);
        lifecycleHooks.registerListener(this);
    }

    @Override
    public void afterRoutesConfigured(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs, Router router) {
        router.get("/system/store/:storeName").handler(handleLoadAll(imposterConfig, allPluginConfigs));
        router.delete("/system/store/:storeName").handler(handleDeleteStore(imposterConfig, allPluginConfigs));
        router.get("/system/store/:storeName/:key").handler(handleLoadSingle(imposterConfig, allPluginConfigs));
        router.put("/system/store/:storeName/:key").handler(handleSaveSingle(imposterConfig, allPluginConfigs));
        router.post("/system/store/:storeName").handler(handleSaveMultiple(imposterConfig, allPluginConfigs));
        router.delete("/system/store/:storeName/:key").handler(handleDeleteSingle(imposterConfig, allPluginConfigs));
    }

    private Handler<RoutingContext> handleLoadAll(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs) {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx, routingContext -> {
            final String storeName = routingContext.pathParam("storeName");
            final Store store = openStore(routingContext, storeName);
            if (isNull(store)) {
                return;
            }

            final List<MIMEHeader> accepted = routingContext.parsedHeaders().accept();
            if (accepted.isEmpty() || accepted.stream().anyMatch(a -> a.isMatchedBy(JSON_MIME))) {
                LOGGER.debug("Listing store: {}", storeName);
                serialiseBodyAsJson(routingContext, store.loadAll());

            } else {
                // client doesn't accept JSON
                LOGGER.warn("Cannot serialise store: {} as client does not accept JSON", storeName);
                routingContext.response()
                        .setStatusCode(HttpUtil.HTTP_NOT_ACCEPTABLE)
                        .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                        .end("Stores are only available as JSON. Please set an appropriate Accept header.");
            }
        });
    }

    private Handler<RoutingContext> handleDeleteStore(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs) {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx, routingContext -> {
            final String storeName = routingContext.pathParam("storeName");

            storeLocator.deleteStoreByName(storeName);
            LOGGER.debug("Deleted store: {}", storeName);

            routingContext.response()
                    .setStatusCode(HttpUtil.HTTP_NO_CONTENT)
                    .end();
        });
    }

    private Handler<RoutingContext> handleLoadSingle(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs) {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx, routingContext -> {
            final String storeName = routingContext.pathParam("storeName");
            final Store store = openStore(routingContext, storeName);
            if (isNull(store)) {
                return;
            }

            final String key = routingContext.pathParam("key");
            final String value = store.load(key);
            if (nonNull(value)) {
                LOGGER.debug("Returning item: {} from store: {}", key, storeName);
                routingContext.response()
                        .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                        .end(value);
            } else {
                LOGGER.debug("Nonexistent item: {} in store: {}", key, storeName);
                routingContext.response()
                        .setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                        .end();
            }
        });
    }

    private Handler<RoutingContext> handleSaveSingle(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs) {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx, routingContext -> {
            final String storeName = routingContext.pathParam("storeName");
            final Store store = openStore(routingContext, storeName, true);
            if (isNull(store)) {
                return;
            }

            final String key = routingContext.pathParam("key");

            // "If the target resource does not have a current representation and the
            // PUT successfully creates one, then the origin server MUST inform the
            // user agent by sending a 201 (Created) response."
            // See: https://datatracker.ietf.org/doc/html/rfc7231#section-4.3.4
            final int statusCode = store.hasItemWithKey(key) ? HttpUtil.HTTP_OK : HttpUtil.HTTP_CREATED;

            final String value = routingContext.getBodyAsString();
            store.save(key, value);

            LOGGER.debug("Saved item: {} to store: {}", key, storeName);
            routingContext.response()
                    .setStatusCode(statusCode)
                    .end();
        });
    }

    private Handler<RoutingContext> handleSaveMultiple(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs) {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx, routingContext -> {
            final String storeName = routingContext.pathParam("storeName");
            final Store store = openStore(routingContext, storeName, true);
            if (isNull(store)) {
                return;
            }

            final Map<String, Object> items = routingContext.getBodyAsJson().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            items.forEach(store::save);
            LOGGER.debug("Saved {} items to store: {}", items.size(), storeName);

            routingContext.response()
                    .setStatusCode(HttpUtil.HTTP_OK)
                    .end();
        });
    }

    private Handler<RoutingContext> handleDeleteSingle(ImposterConfig imposterConfig, List<PluginConfig> allPluginConfigs) {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx, routingContext -> {
            final String storeName = routingContext.pathParam("storeName");
            final Store store = openStore(routingContext, storeName, true);
            if (isNull(store)) {
                return;
            }

            final String key = routingContext.pathParam("key");
            store.delete(key);

            LOGGER.debug("Deleted item: {} from store: {}", key, storeName);
            routingContext.response()
                    .setStatusCode(HttpUtil.HTTP_NO_CONTENT)
                    .end();
        });
    }

    private Store openStore(RoutingContext routingContext, String storeName) {
        return openStore(routingContext, storeName, false);
    }

    private Store openStore(RoutingContext routingContext, String storeName, boolean createIfNotExist) {
        if (!storeLocator.hasStoreWithName(storeName)) {
            LOGGER.debug("No store found named: {}", storeName);

            if (!createIfNotExist) {
                routingContext.response()
                        .setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                        .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                        .end(String.format("No store named '%s'.", storeName));
                return null;
            }
            // ...otherwise fall through and implicitly create below
        }

        return storeLocator.getStoreByName(storeName);
    }

    private void serialiseBodyAsJson(RoutingContext routingContext, Object body) {
        try {
            routingContext.response()
                    .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
                    .end(MapUtil.JSON_MAPPER.writeValueAsString(body));

        } catch (JsonProcessingException e) {
            routingContext.fail(e);
        }
    }

    @Override
    public void beforeBuildingRuntimeContext(Map<String, Object> additionalBindings, ExecutionContext executionContext) {
        additionalBindings.put("stores", storeHolder);
    }

    @Override
    public void afterSuccessfulScriptExecution(Map<String, Object> additionalBindings, ReadWriteResponseBehaviour responseBehaviour) {
        // no op
    }
}
