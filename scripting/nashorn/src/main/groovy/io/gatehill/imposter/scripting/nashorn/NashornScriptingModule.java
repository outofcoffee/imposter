package io.gatehill.imposter.scripting.nashorn;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import io.gatehill.imposter.scripting.nashorn.service.NashhornScriptServiceImpl;
import io.gatehill.imposter.service.ScriptService;
import io.gatehill.imposter.util.annotation.JavascriptImpl;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class NashornScriptingModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ScriptService.class).annotatedWith(JavascriptImpl.class).to(NashhornScriptServiceImpl.class).in(Singleton.class);
    }
}
