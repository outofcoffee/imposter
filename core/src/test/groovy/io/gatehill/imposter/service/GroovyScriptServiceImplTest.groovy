package io.gatehill.imposter.service

import javax.inject.Inject

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class GroovyScriptServiceImplTest extends AbstractScriptServiceImplTest {
    @Inject
    private GroovyScriptServiceImpl service;

    @Override
    protected ScriptService getService() {
        return service
    }

    @Override
    protected String getScriptName() {
        return 'test.groovy'
    }
}
