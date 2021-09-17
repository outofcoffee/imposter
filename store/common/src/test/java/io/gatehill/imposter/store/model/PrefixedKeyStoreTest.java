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

import io.gatehill.imposter.store.inmem.InMemoryStore;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link PrefixedKeyStore}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class PrefixedKeyStoreTest {
    private InMemoryStore delegateStore;
    private PrefixedKeyStore store;

    @Before
    public void setUp() throws Exception {
        delegateStore = new InMemoryStore("test");
        store = new PrefixedKeyStore("pref.", delegateStore);
    }

    @Test
    public void testPrefixedKeys() {
        store.save("foo", "bar");
        assertTrue(delegateStore.hasItemWithKey("pref.foo"));
        assertEquals(store.load("foo"), "bar");

        // prefix should not be present in keys
        final Set<String> allKeys = store.loadAll().keySet();
        assertThat(allKeys, not(hasItem("pref.foo")));
        assertThat(allKeys, hasItem("foo"));
    }
}
