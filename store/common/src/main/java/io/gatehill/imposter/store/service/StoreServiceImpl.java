package io.gatehill.imposter.store.service;

import io.gatehill.imposter.script.ExecutionContext;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;
import io.gatehill.imposter.service.ResponseService;
import io.gatehill.imposter.store.impl.AbstractStoreLocator;
import io.gatehill.imposter.store.model.StoreHolder;
import io.gatehill.imposter.store.model.StoreLocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class StoreServiceImpl implements StoreService, ResponseService.ScriptedResponseListener {
    private static final Logger LOGGER = LogManager.getLogger(StoreServiceImpl.class);

    private final StoreHolder storeHolder;

    @Inject
    public StoreServiceImpl(ResponseService responseService, StoreLocator storeLocator) {
        LOGGER.warn("Experimental store support enabled");

        storeHolder = new StoreHolder(storeLocator);
        responseService.registerListener(this);
    }

    @Override
    public void beforeBuildingRuntimeContext(Map<String, Object> additionalBindings, ExecutionContext executionContext) {
        additionalBindings.put("stores", storeHolder);
    }

    @Override
    public void afterSuccessfulExecution(Map<String, Object> additionalBindings, ReadWriteResponseBehaviour responseBehaviour) {
        // no op
    }
}
