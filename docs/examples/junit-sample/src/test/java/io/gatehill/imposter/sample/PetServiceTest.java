/*
 * Copyright (c) 2021.
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

package io.gatehill.imposter.sample;

import io.gatehill.imposter.openapi.embedded.OpenApiImposterBuilder;
import io.gatehill.imposter.openapi.embedded.OpenApiMockEngine;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for pet service that calls the pet API.
 */
public class PetServiceTest {
    private PetService petService;

    @Before
    public void setUp() throws Exception {
        Path specFile = Paths.get(PetServiceTest.class.getResource("/config/petstore-simple.yaml").toURI());

        OpenApiMockEngine imposter = new OpenApiImposterBuilder<>()
                .withSpecificationFile(specFile)
                .startBlocking();

        URL mockEndpoint = imposter.getBaseUrl();
        petService = new PetService(mockEndpoint);
    }

    /**
     * The service should return the pets from the API.
     */
    @Test
    public void shouldListPets() throws Exception {
        final Pet[] pets = petService.listPets();

        assertEquals(2, pets.length);
        assertEquals("Cat", pets[0].getName());
    }
}
