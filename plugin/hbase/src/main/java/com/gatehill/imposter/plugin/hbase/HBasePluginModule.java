package com.gatehill.imposter.plugin.hbase;

import com.gatehill.imposter.plugin.hbase.service.ScannerService;
import com.gatehill.imposter.plugin.hbase.service.ScannerServiceImpl;
import com.gatehill.imposter.plugin.hbase.service.serialisation.DeserialisationService;
import com.gatehill.imposter.plugin.hbase.service.serialisation.JsonSerialisationServiceImpl;
import com.gatehill.imposter.plugin.hbase.service.serialisation.ProtobufSerialisationServiceImpl;
import com.gatehill.imposter.plugin.hbase.service.serialisation.SerialisationService;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

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
