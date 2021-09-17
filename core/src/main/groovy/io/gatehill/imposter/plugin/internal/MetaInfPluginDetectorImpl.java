/*
 * Copyright (c) 2016-2021.
 *
 * This file is part of Imposter.
 *
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as
 * defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software: Imposter
 *
 * License: GNU Lesser General Public License version 3
 *
 * Licensor: Peter Cornish
 *
 * Imposter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Imposter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.gatehill.imposter.plugin.internal;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.plugin.PluginInfo;
import io.gatehill.imposter.plugin.PluginProvider;
import io.gatehill.imposter.util.MetaUtil;
import io.vertx.ext.web.Router;

import java.io.File;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

/**
 * Provides the plugin class names present in the metadata information ('META-INF') files.
 * <p>
 * Checks for a list of comma-separated plugin names or fully qualified classes.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@PluginInfo("meta-detector")
public class MetaInfPluginDetectorImpl implements Plugin, PluginProvider {
    @Override
    public void configureRoutes(Router router) {
        // no op
    }

    @Override
    public List<String> providePlugins(ImposterConfig imposterConfig, Map<String, List<File>> pluginConfigs) {
        return ofNullable(MetaUtil.readMetaProperties().getProperty("plugins"))
                .map(plugin -> plugin.split(","))
                .map(elements -> (List<String>) newArrayList(elements))
                .orElse(emptyList());
    }
}
