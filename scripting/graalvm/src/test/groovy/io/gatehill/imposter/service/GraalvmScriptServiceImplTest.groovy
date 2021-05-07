package io.gatehill.imposter.service


import io.gatehill.imposter.scripting.AbstractScriptServiceImplTest
import io.gatehill.imposter.scripting.graalvm.service.GraalvmScriptServiceImpl

import javax.inject.Inject

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class GraalvmScriptServiceImplTest extends AbstractScriptServiceImplTest {
    @Inject
    private GraalvmScriptServiceImpl service;

    @Override
    protected ScriptService getService() {
        return service
    }

    @Override
    protected String getScriptName() {
        return 'test.js'
    }
}
