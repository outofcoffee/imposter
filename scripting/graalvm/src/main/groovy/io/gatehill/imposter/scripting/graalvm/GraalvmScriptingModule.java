package io.gatehill.imposter.scripting.graalvm;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import io.gatehill.imposter.scripting.graalvm.service.GraalvmScriptServiceImpl;
import io.gatehill.imposter.service.ScriptService;
import io.gatehill.imposter.util.annotation.JavascriptImpl;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class GraalvmScriptingModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ScriptService.class).annotatedWith(JavascriptImpl.class).to(GraalvmScriptServiceImpl.class).in(Singleton.class);
    }
}
