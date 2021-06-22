package io.gatehill.imposter.service

import io.gatehill.imposter.scripting.AbstractScriptServiceImplTest
import io.gatehill.imposter.scripting.nashorn.service.NashornScriptServiceImpl

import javax.inject.Inject

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class NashornScriptServiceImplTest extends AbstractScriptServiceImplTest {
    @Inject
    private NashornScriptServiceImpl service;

    @Override
    protected ScriptService getService() {
        return service
    }

    @Override
    protected String getScriptName() {
        return 'test.js'
    }
}
