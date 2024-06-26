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
package io.gatehill.imposter.store.dynamodb.config

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.BasicSessionCredentials
import com.amazonaws.regions.DefaultAwsRegionProviderChain
import io.gatehill.imposter.config.util.EnvVars
import java.util.Objects.isNull

/**
 * @author Pete Cornish
 */
internal object Settings {
    val awsCredentials: AWSCredentials?
        get() {
            val accessKey = EnvVars.getEnv("AWS_ACCESS_KEY_ID")
            val secretKey = EnvVars.getEnv("AWS_SECRET_ACCESS_KEY")
            if (isNull(accessKey) || isNull(secretKey)) {
                return null
            }
            val sessionKey = EnvVars.getEnv("AWS_SESSION_TOKEN")
            return sessionKey?.let {
                BasicSessionCredentials(accessKey, secretKey, sessionKey)
            } ?: run {
                BasicAWSCredentials(accessKey, secretKey)
            }
        }

    val dynamoDbApiEndpoint: String?
        get() = EnvVars.getEnv("IMPOSTER_DYNAMODB_ENDPOINT")

    val dynamoDbRegion: String
        get() = EnvVars.getEnv("IMPOSTER_DYNAMODB_REGION") ?: DefaultAwsRegionProviderChain().region

    val tableName: String
        get() = EnvVars.getEnv("IMPOSTER_DYNAMODB_TABLE") ?: "Imposter"

    val objectSerialisation: ObjectSerialisation
        get() = EnvVars.getEnv("IMPOSTER_DYNAMODB_OBJECT_SERIALISATION")?.let { ObjectSerialisation.valueOf(it) }
            ?: ObjectSerialisation.BINARY

    object Ttl {
        private const val disabledValue = -1L

        val enabled: Boolean
            get() = seconds != disabledValue

        val seconds: Long
            get() {
                return EnvVars.getEnv("IMPOSTER_DYNAMODB_TTL")?.toLong() ?: disabledValue
            }

        val attributeName: String
            get() = EnvVars.getEnv("IMPOSTER_DYNAMODB_TTL_ATTRIBUTE") ?: "ttl"
    }

    enum class ObjectSerialisation {
        BINARY,
        MAP,
    }
}
