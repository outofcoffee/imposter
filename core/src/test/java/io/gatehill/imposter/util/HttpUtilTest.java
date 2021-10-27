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

package io.gatehill.imposter.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link HttpUtil}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class HttpUtilTest {
    @Test
    public void readAcceptedContentTypes() throws Exception {
        // note: provided out of order, but should be sorted by 'q' weight
        final String acceptHeader = "text/html; q=1.0, text/*; q=0.8, image/jpeg; q=0.5, image/gif; q=0.7, image/*; q=0.4, */*; q=0.1";

        final List<String> actual = HttpUtil.readAcceptedContentTypes(acceptHeader);
        assertNotNull(actual);
        assertEquals(6, actual.size());

        // check order
        assertEquals("text/html", actual.get(0));
        assertEquals("text/*", actual.get(1));
        assertEquals("image/gif", actual.get(2));
        assertEquals("image/jpeg", actual.get(3));
        assertEquals("image/*", actual.get(4));
        assertEquals("*/*", actual.get(5));
    }
}
