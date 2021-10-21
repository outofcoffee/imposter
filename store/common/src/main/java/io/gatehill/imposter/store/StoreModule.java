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

package io.gatehill.imposter.store;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import io.gatehill.imposter.store.inmem.InMemoryStoreModule;
import io.gatehill.imposter.store.service.StoreService;
import io.gatehill.imposter.store.service.StoreServiceImpl;
import io.gatehill.imposter.util.EnvVars;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class StoreModule extends AbstractModule {
    private static final String DEFAULT_STORE_MODULE = InMemoryStoreModule.class.getCanonicalName();
    private static final Logger LOGGER = LogManager.getLogger(StoreModule.class);

    @Override
    protected void configure() {
        // needs to be eager to register lifecycle listener
        bind(StoreService.class).to(StoreServiceImpl.class).asEagerSingleton();

        install(discoverStoreModule());
    }

    @SuppressWarnings("unchecked")
    private Module discoverStoreModule() {
        final String storeModule = ofNullable(EnvVars.getEnv("IMPOSTER_STORE_MODULE")).orElse(DEFAULT_STORE_MODULE);
        LOGGER.trace("Loading store module: {}", storeModule);
        try {
            final Class<? extends Module> moduleClass = (Class<? extends Module>) Class.forName(storeModule);
            return moduleClass.newInstance();

        } catch (Exception e) {
            throw new RuntimeException("Unable to load store module: " + storeModule +
                    ". Must be a fully qualified class implementing " + Module.class.getCanonicalName() +
                    " with a no-arg constructor.", e);
        }
    }
}
