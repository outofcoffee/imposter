package io.gatehill.imposter.store.graphql

import io.gatehill.imposter.plugin.Plugin
import io.gatehill.imposter.plugin.PluginInfo
import io.gatehill.imposter.plugin.RequireModules

@PluginInfo("store-graphql")
@RequireModules(GraphQLQueryModule::class)
class GraphQLQueryPlugin : Plugin
