/*
 * Copyright (c) 2023-2023.
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

package io.gatehill.imposter.plugin.fakedata

import net.datafaker.Faker

/**
 * Generates fake data.
 */
object FakeGenerator {
    private val faker = Faker()

    fun expression(expression: String): String? {
        try {
            return faker.expression("#{$expression}")
        } catch (e: Exception) {
            throw RuntimeException("Failed to evaluate fake data expression: $expression", e)
        }
    }

    /**
     * Generates a fake value for the given property name, or `null` if
     * no fake value is available for the given property name.
     */
    fun fake(propNameHint: String): String? = when (propNameHint.lowercase()) {
        "email", "emailaddress" -> faker.internet().emailAddress()
        "firstname" -> faker.name().firstName()
        "lastname", "surname" -> faker.name().lastName()
        "fullname", "name" -> faker.name().fullName()
        "username" -> faker.name().username()
        "password" -> faker.internet().password()
        "address", "fulladdress" -> faker.address().fullAddress()
        "streetaddress", "street" -> faker.address().streetAddress()
        "city" -> faker.address().city()
        "country" -> faker.address().country()
        "zipcode" -> faker.address().zipCode()
        "phonenumber" -> faker.phoneNumber().phoneNumber()
        "postcode" -> faker.address().postcode()
        else -> null
    }
}
