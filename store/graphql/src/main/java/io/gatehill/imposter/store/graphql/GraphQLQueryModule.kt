package io.gatehill.imposter.store.graphql

import com.google.inject.AbstractModule
import io.gatehill.imposter.util.FeatureUtil
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class GraphQLQueryModule : AbstractModule() {
    private val logger : Logger = LogManager.getLogger(GraphQLQueryModule::class.java)

    override fun configure() {
        if (FeatureUtil.isFeatureEnabled("stores")) {
            // eager to bind lifecycle hook
            bind(GraphQLQueryService::class.java).asEagerSingleton()
        } else {
            logger.debug("Skipping GraphQL plugin initialisation as stores feature is disabled")
        }
    }
}
