package io.gatehill.imposter.scripting.groovy;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import io.gatehill.imposter.scripting.groovy.service.GroovyScriptServiceImpl;
import io.gatehill.imposter.service.ScriptService;
import io.gatehill.imposter.util.annotation.GroovyImpl;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class GroovyScriptingModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ScriptService.class).annotatedWith(GroovyImpl.class).to(GroovyScriptServiceImpl.class).in(Singleton.class);
    }
}
