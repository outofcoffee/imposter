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

package io.gatehill.imposter.store.model;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * A delegating {@link Store} wrapper that prepends a string to item keys
 * before persistence and retrieval.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class PrefixedKeyStore implements Store {
    private final String keyPrefix;
    private final Store delegate;

    public PrefixedKeyStore(String keyPrefix, Store delegate) {
        this.keyPrefix = keyPrefix;
        this.delegate = delegate;
    }

    @Override
    public String getStoreName() {
        return delegate.getStoreName();
    }

    private String buildKey(String key) {
        return keyPrefix + key;
    }

    @Override
    public String getTypeDescription() {
        return delegate.getTypeDescription();
    }

    @Override
    public void save(String key, Object value) {
        delegate.save(buildKey(key), value);
    }

    @Override
    public <T> T load(String key) {
        return delegate.load(buildKey(key));
    }

    @Override
    public void delete(String key) {
        delegate.delete(buildKey(key));
    }

    @Override
    public Map<String, Object> loadAll() {
        // strip out key prefix
        return delegate.loadAll().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().substring(keyPrefix.length()), Map.Entry::getValue));
    }

    @Override
    public boolean hasItemWithKey(String key) {
        return delegate.hasItemWithKey(buildKey(key));
    }

    @Override
    public int count() {
        return delegate.count();
    }
}
