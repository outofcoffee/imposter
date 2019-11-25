package io.gatehill.imposter.service

import javax.inject.Inject

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class NashhornScriptServiceImplTest extends AbstractScriptServiceImplTest {
    @Inject
    private NashhornScriptServiceImpl service;

    @Override
    protected ScriptService getService() {
        return service
    }

    @Override
    protected String getScriptName() {
        return 'test.js'
    }
}
