package io.gatehill.imposter.plugin.hbase;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import io.gatehill.imposter.plugin.hbase.service.ScannerService;
import io.gatehill.imposter.plugin.hbase.service.ScannerServiceImpl;
import io.gatehill.imposter.plugin.hbase.service.serialisation.DeserialisationService;
import io.gatehill.imposter.plugin.hbase.service.serialisation.JsonSerialisationServiceImpl;
import io.gatehill.imposter.plugin.hbase.service.serialisation.ProtobufSerialisationServiceImpl;
import io.gatehill.imposter.plugin.hbase.service.serialisation.SerialisationService;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class HBasePluginModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ScannerService.class).to(ScannerServiceImpl.class).in(Singleton.class);

        bind(SerialisationService.class).annotatedWith(Names.named("application/x-protobuf")).to(ProtobufSerialisationServiceImpl.class).in(Singleton.class);
        bind(DeserialisationService.class).annotatedWith(Names.named("application/x-protobuf")).to(ProtobufSerialisationServiceImpl.class).in(Singleton.class);

        bind(SerialisationService.class).annotatedWith(Names.named("application/json")).to(JsonSerialisationServiceImpl.class).in(Singleton.class);
        bind(DeserialisationService.class).annotatedWith(Names.named("application/json")).to(JsonSerialisationServiceImpl.class).in(Singleton.class);

        bind(ScannerService.class).to(ScannerServiceImpl.class).in(Singleton.class);
    }
}
